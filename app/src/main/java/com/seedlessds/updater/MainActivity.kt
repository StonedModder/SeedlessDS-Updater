package com.seedlessds.updater

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.seedlessds.updater.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    companion object {
        private const val GITHUB_API = "https://api.github.com/repos/SapphireRhodonite/SeedlessDS/releases/latest"
        private const val SEEDLESS_PACKAGE = "com.seedlessds.app"
        private const val REQUEST_INSTALL_PACKAGES = 1001
        private const val REQUEST_STORAGE_PERMISSION = 1002
    }

    private lateinit var binding: ActivityMainBinding
    private var latestApkUrl: String? = null
    private var latestVersion: String? = null
    private var downloadJob: Job? = null
    private var permissionGranted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkSeedlessInstalled()
        setupButtons()
        fetchLatestRelease()
    }

    private fun checkSeedlessInstalled() {
        val info = try {
            packageManager.getPackageInfo(SEEDLESS_PACKAGE, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }

        if (info != null) {
            binding.tvStatus.text = getString(R.string.status_update_available, info.versionName ?: "?")
            binding.btnAction.text = getString(R.string.btn_update)
        } else {
            binding.tvStatus.text = getString(R.string.status_not_installed)
            binding.btnAction.text = getString(R.string.btn_install)
        }
    }

    private fun setupButtons() {
        binding.btnAction.setOnClickListener {
            if (latestApkUrl != null) {
                checkPermissionsAndDownload()
            } else {
                binding.tvStatus.text = getString(R.string.status_release_not_ready)
                fetchLatestRelease()
            }
        }

        binding.btnCheck.setOnClickListener {
            binding.tvProgressText.text = ""
            binding.progressBar.progress = 0
            binding.progressBar.visibility = android.view.View.VISIBLE
            binding.progressBar.isIndeterminate = true
            binding.tvVersion.text = ""
            fetchLatestRelease()
        }
    }

    private fun fetchLatestRelease() {
        binding.btnAction.isEnabled = false
        binding.btnCheck.isEnabled = false
        binding.tvStatus.text = getString(R.string.status_checking)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val connection = URL(GITHUB_API).openConnection() as HttpURLConnection
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                if (connection.responseCode != 200) {
                    throw Exception("GitHub API returned ${connection.responseCode}")
                }

                val json = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val release = JSONObject(json)
                val tagName = release.getString("tag_name")
                val assets = release.getJSONArray("assets")
                var apkUrl: String? = null

                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.getString("name").endsWith(".apk")) {
                        apkUrl = asset.getString("browser_download_url")
                        break
                    }
                }

                withContext(Dispatchers.Main) {
                    if (apkUrl != null) {
                        latestApkUrl = apkUrl
                        latestVersion = tagName
                        binding.tvVersion.text = getString(R.string.version_latest, tagName)
                        binding.btnAction.isEnabled = true

                        val info = try {
                            packageManager.getPackageInfo(SEEDLESS_PACKAGE, 0)
                        } catch (_: Exception) { null }

                        if (info != null) {
                            val current = info.versionName
                            if (current != null && (current == tagName || current.contains(tagName))) {
                                binding.tvStatus.text = getString(R.string.status_up_to_date)
                                binding.btnAction.text = getString(R.string.btn_reinstall)
                            } else {
                                binding.tvStatus.text = getString(R.string.status_update_available, current ?: "?")
                                binding.btnAction.text = getString(R.string.btn_update)
                            }
                        } else {
                            binding.tvStatus.text = getString(R.string.status_ready_to_install)
                            binding.btnAction.text = getString(R.string.btn_install)
                        }

                        if (permissionGranted) {
                            startDownload()
                        }
                    } else {
                        binding.tvVersion.text = getString(R.string.version_not_found)
                        binding.tvStatus.text = getString(R.string.status_no_apk_found)
                    }
                    binding.progressBar.isIndeterminate = false
                    binding.progressBar.progress = 0
                    binding.progressBar.visibility = android.view.View.INVISIBLE
                    binding.btnCheck.isEnabled = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvVersion.text = getString(R.string.version_error)
                    binding.tvStatus.text = getString(R.string.status_api_error, e.message ?: "unknown")
                    binding.progressBar.isIndeterminate = false
                    binding.progressBar.visibility = android.view.View.INVISIBLE
                    binding.btnCheck.isEnabled = true
                }
            }
        }
    }

    private fun checkPermissionsAndDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                binding.tvStatus.text = getString(R.string.status_need_permission)
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivityForResult(intent, REQUEST_INSTALL_PACKAGES)
                return
            }
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_STORAGE_PERMISSION
                )
                return
            }
        }

        permissionGranted = true
        if (latestApkUrl != null) {
            startDownload()
        } else {
            binding.tvStatus.text = getString(R.string.status_fetching_first)
            fetchLatestRelease()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_INSTALL_PACKAGES) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (packageManager.canRequestPackageInstalls()) {
                    permissionGranted = true
                    if (latestApkUrl != null) {
                        startDownload()
                    } else {
                        binding.tvStatus.text = getString(R.string.status_fetching_first)
                        fetchLatestRelease()
                    }
                } else {
                    binding.tvStatus.text = getString(R.string.status_permission_denied)
                    Toast.makeText(this, R.string.toast_need_permission, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionGranted = true
                if (latestApkUrl != null) {
                    startDownload()
                } else {
                    binding.tvStatus.text = getString(R.string.status_fetching_first)
                    fetchLatestRelease()
                }
            } else {
                binding.tvStatus.text = getString(R.string.status_permission_denied)
            }
        }
    }

    private fun startDownload() {
        val url = latestApkUrl ?: return
        val version = latestVersion ?: "latest"
        val apkName = "SeedlessDS-${version}.apk"
        val destFile = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), apkName)
            }
            else -> {
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), apkName)
            }
        }

        destFile.parentFile?.mkdirs()
        if (destFile.exists()) destFile.delete()

        binding.tvStatus.text = getString(R.string.status_downloading)
        binding.tvProgressText.text = getString(R.string.progress_connecting)
        binding.progressBar.isIndeterminate = false
        binding.progressBar.progress = 0
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnAction.isEnabled = false

        downloadJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 60000
                connection.instanceFollowRedirects = true

                if (connection.responseCode != 200) {
                    throw Exception("HTTP ${connection.responseCode}")
                }

                val contentLength = connection.contentLength
                val total: Long = if (contentLength > 0) contentLength.toLong() else -1L

                withContext(Dispatchers.Main) {
                    binding.tvProgressText.text = if (total > 0) {
                        getString(R.string.progress_downloading, "0", formatSize(total))
                    } else {
                        getString(R.string.progress_downloading_unknown)
                    }
                }

                val input: InputStream = connection.inputStream
                val output = FileOutputStream(destFile)
                val buffer = ByteArray(8192)
                var downloaded: Long = 0
                var lastUpdate = 0L
                var bytes: Int

                while (input.read(buffer).also { bytes = it } != -1) {
                    output.write(buffer, 0, bytes)
                    downloaded += bytes

                    val now = System.currentTimeMillis()
                    if (now - lastUpdate > 200 || downloaded == total) {
                        lastUpdate = now
                        val pct = if (total > 0) ((downloaded * 100) / total).toInt() else -1
                        withContext(Dispatchers.Main) {
                            if (pct >= 0) {
                                binding.progressBar.progress = pct
                                binding.tvProgressText.text = getString(
                                    R.string.progress_percent,
                                    pct,
                                    formatSize(downloaded),
                                    formatSize(total)
                                )
                            } else {
                                binding.tvProgressText.text = getString(
                                    R.string.progress_bytes,
                                    formatSize(downloaded)
                                )
                            }
                        }
                    }
                }

                output.flush()
                output.close()
                input.close()
                connection.disconnect()

                withContext(Dispatchers.Main) {
                    binding.progressBar.progress = 100
                    binding.progressBar.visibility = android.view.View.INVISIBLE
                    binding.tvProgressText.text = getString(R.string.progress_complete)
                    binding.tvStatus.text = getString(R.string.status_install_ready)
                    installApk(destFile)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = android.view.View.INVISIBLE
                    binding.tvProgressText.text = ""
                    binding.tvStatus.text = getString(R.string.status_download_failed_detail, e.message ?: "unknown")
                    binding.btnAction.isEnabled = true
                    Toast.makeText(this@MainActivity, R.string.toast_download_failed, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun installApk(file: File) {
        if (!file.exists() || file.length() == 0L) {
            binding.tvStatus.text = getString(R.string.status_file_missing)
            binding.btnAction.isEnabled = true
            return
        }

        try {
            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            } else {
                Uri.fromFile(file)
            }

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
                binding.tvStatus.text = getString(R.string.status_need_permission)
                val permIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivityForResult(permIntent, REQUEST_INSTALL_PACKAGES)
                return
            }

            startActivity(installIntent)
            binding.tvStatus.text = getString(R.string.status_installing)
            binding.btnAction.isEnabled = true
        } catch (e: Exception) {
            binding.tvStatus.text = getString(R.string.status_install_error_detail, e.message ?: "")
            binding.btnAction.isEnabled = true
        }
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
            else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
        }
    }

    override fun onDestroy() {
        downloadJob?.cancel()
        super.onDestroy()
    }
}
