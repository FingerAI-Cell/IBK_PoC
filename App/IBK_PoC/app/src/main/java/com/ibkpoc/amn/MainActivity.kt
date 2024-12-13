package com.ibkpoc.amn

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import com.ibkpoc.amn.ui.screens.main.MainScreen
import com.ibkpoc.amn.ui.theme.MeetingAppTheme
import com.ibkpoc.amn.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    
    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 100
        private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.FOREGROUND_SERVICE,
                Manifest.permission.FOREGROUND_SERVICE_MICROPHONE
            )
        } else {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.FOREGROUND_SERVICE
            )
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // 모든 권한 확인 후 ViewModel 상태 업데이트
        val allPermissionsGranted = REQUIRED_PERMISSIONS.all {
            permissions[it] == true
        }
        viewModel.setHasRecordPermission(allPermissionsGranted)

        if (allPermissionsGranted) {
            startApp()
        } else {
            handlePermissionDenied()
        }
    }

    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle("권한 필요")
            .setMessage("음성 녹음 및 파일 저장을 위해 권한이 필요합니다.\n권한을 허용하시겠습니까?")
            .setPositiveButton("권한 설정") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                checkPermissions()
            }
            .setNegativeButton("취소") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                Toast.makeText(this, "권한이 거부되어 일부 기능을 사용할 수 없습니다.", Toast.LENGTH_LONG).show()
                startApp()  // 제한된 기능으로 앱 시작
            }
            .setCancelable(false)
            .show()
    }

    private fun showPermissionSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("권한 설정 필요")
            .setMessage("앱 설정에서 필요한 권한을 허용해주세요.")
            .setPositiveButton("설정으로 이동") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                openAppSettings()
            }
            .setNegativeButton("취소") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                Toast.makeText(this, "권한이 거부되어 일부 기능을 사용할 수 없습니다.", Toast.LENGTH_LONG).show()
                startApp()  // 제한된 기능으로 앱 시작
            }
            .setCancelable(false)
            .show()
    }

    private fun openAppSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            startActivity(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 앱 실행 시 권한 확인 및 초기화
        if (hasAllPermissions()) {
            viewModel.setHasRecordPermission(true)
            startApp()
        } else {
            checkPermissions()
        }
    }

    private fun startApp() {
        setContent {
            MeetingAppTheme {
                val recordingState = viewModel.recordingState.collectAsState().value
                val errorMessage = viewModel.errorMessage.collectAsState().value
                val isLoading = viewModel.isLoading.collectAsState().value
                
                MainScreen(
                    recordingState = recordingState,
                    errorMessage = errorMessage,
                    isLoading = isLoading,
                    onStartMeeting = { viewModel.startMeeting() },
                    onEndMeeting = { viewModel.endMeeting() }
                )
            }
        }
    }

    private fun checkPermissions() {
        val missingPermissions = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            viewModel.setHasRecordPermission(true)
            startApp()
        }
    }

    private fun hasAllPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun handlePermissionDenied() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
            showPermissionExplanationDialog()
        } else {
            showPermissionSettingsDialog()
        }
    }
}

