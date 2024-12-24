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
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.launch
import com.ibkpoc.amn.model.RecordServiceState
import com.ibkpoc.amn.service.AudioRecordService
import com.ibkpoc.amn.ui.screens.main.MainScreen
import com.ibkpoc.amn.ui.theme.MeetingAppTheme
import com.ibkpoc.amn.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.ibkpoc.amn.event.RecordingStateEvent
import com.ibkpoc.amn.event.EventBus

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    
    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 100
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO  // 음성 권한만 남김
        )
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
            .setPositiveButton("권한 수정") { dialog: DialogInterface, _: Int ->
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
        
        // LocalBroadcastManager 대신 EventBus 사용
        lifecycleScope.launch {
            EventBus.subscribe<RecordingStateEvent>().collect { event ->
                // 상태 처리
                when (val state = event.state) {
                    is RecordServiceState.Error -> {
                        Toast.makeText(
                            this@MainActivity,
                            state.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    is RecordServiceState.Completed -> {
                        // 녹음 완료 처리
                    }
                    else -> { /* 다른 상태 처리 */ }
                }
            }
        }

        // 앱 실행 시 권한 확인 및 초기화
        if (hasAllPermissions()) {
            viewModel.setHasRecordPermission(true)
            startApp()
        } else {
            checkPermissions()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun startApp() {
        setContent {
            MeetingAppTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    private fun checkPermissions() {
        // 이미 권한이 있는지 먼저 확인
        val missingPermissions = REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        // 필요한 권한만 요청
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                PERMISSIONS_REQUEST_CODE
            )
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

