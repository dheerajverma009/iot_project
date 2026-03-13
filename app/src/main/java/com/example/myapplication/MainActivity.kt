package com.example.myapplication

import android.Manifest
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.jimi.ble.BluetoothConfig
import com.jimi.ble.BluetoothLESDK
import com.jimi.ble.bean.BaseObdData
import com.jimi.ble.command.checkPasswordEnable
import com.jimi.ble.command.disablePassword
import com.jimi.ble.command.queryHistoryData
import com.jimi.ble.command.queryTerminalInfo
import com.jimi.ble.command.replyReceiveCustomCommandReply
import com.jimi.ble.command.replyReceivedEldData
import com.jimi.ble.command.replyReceivedHistoryProgress
import com.jimi.ble.command.replyReceivedObdData
import com.jimi.ble.command.sendUTCTime
import com.jimi.ble.command.startReportEldData
import com.jimi.ble.command.validatePassword
import com.jimi.ble.decode.getCustomCommandReply
import com.jimi.ble.decode.getDriverAuthInfo
import com.jimi.ble.decode.getHistoryDataCount
import com.jimi.ble.decode.getHistoryDataProgress
import com.jimi.ble.decode.getObdDataItemConfig
import com.jimi.ble.decode.getTerminalInfo
import com.jimi.ble.decode.queryHistoryDataSuccess
import com.jimi.ble.entity.BleDevice
import com.jimi.ble.entity.parse.BtParseData
import com.jimi.ble.interfaces.OnBluetoothGattCallback
import com.jimi.ble.interfaces.OnBluetoothScanCallback
import com.jimi.ble.interfaces.ProtocolParseData
import com.jimi.ble.protocol.EtProtocol
import com.jimi.ble.protocol.ObdProtocol
import com.jimi.ble.utils.InstructionAnalysis
import com.jimi.ble.utils.expand.getInt

class MainActivity : ComponentActivity() {
    ///without vehicle: C4:A8:28:44:BB:D5
    ///pin code - 000552

    /// active vehicle pin code - 131074
    private var mac: String = "C4:A8:28:44:BB:D5"
    private var ON_DATA_ANALYZE: String = "ON_DATA_ANALYZE"

    private val TAG: String = "JIMI_IOT_APPLICATION"

    private val permissionsLow = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    @RequiresApi(Build.VERSION_CODES.S)
    private val permissionsHigh = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    // Register the permission launcher [cite: 31]
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.entries.all { it.value }) {
            // All permissions granted, start scanning [cite: 31]
        }
    }

    private val onGattCallback: OnBluetoothGattCallback =
        object : OnBluetoothGattCallback.ParsedBluetoothGattCallback() {
            override fun onConnected() {
                Log.d(TAG, "onGattCallback => onConnected")
            }

            override fun onAuthenticationPassed() {
                Log.d(TAG, "onGattCallback => onAuthenticationPassed")
            }

            override fun onNotifyReceived(data: ProtocolParseData) {
                Log.d(TAG, "onGattCallback => ProtocolParseData ${data.ack}")
                onDataAnalyze(data)
            }

            override fun onDisconnect() {
                Log.d(TAG, "onGattCallback => onDisconnect")
            }

            override fun onConnectFailure(status: Int) {
                BluetoothLESDK.release()
                super.onConnectFailure(status)
            }
        }

    private fun onDataAnalyze(data: ProtocolParseData) {
        when (data.ack) {
            InstructionAnalysis.BT.ACK_OBD_CHECK_PASSWORD_SET -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_CHECK_PASSWORD_SET")
                val isSet = data.source.getInt(8) == 1

                if (isSet) {
//                    BluetoothLESDK.validatePassword(DEVICE_PASSWORD)
                } else {
//                    BluetoothLESDK.enablePassword(DEVICE_PASSWORD)
                }
            }
            InstructionAnalysis.BT.ACK_OBD_VERIFY_PASSWORD -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_VERIFY_PASSWORD")

                /*
                Password verification result from the device
                */
                val success = data.source.getInt(8) == 1
                if (success) {
                    // Password is correct. Proceed with operations (e.g., disable password)
//                    BluetoothLESDK.disablePassword()
                } else {
                    // Otherwise, further interaction with the device is blocked.
                    BluetoothLESDK.close()
                }
            }
            InstructionAnalysis.BT.ACK_OBD_SET_PASSWORD -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_SET_PASSWORD")

                // Response to password enable/disable operation
                val isSuccess = data.source.getInt(8) == 1
                if (isSuccess) {
                    // Handle success case
                } else {
                    // Handle failure case
                }
            }
            InstructionAnalysis.BT.ACK_OBD_REQUEST_TIME -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_REQUEST_TIME")
                BluetoothLESDK.sendUTCTime()
            }
            InstructionAnalysis.BT.ACK_OBD_DISPLAY_START -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_DISPLAY_START")
            }
            InstructionAnalysis.BT.ACK_OBD_DISPLAY_PROCESS -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_DISPLAY_PROCESS")
                BluetoothLESDK.replyReceivedObdData()
            }
            InstructionAnalysis.BT.ACK_OBD_DISPLAY_FINISH -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_DISPLAY_FINISH")
            }
            InstructionAnalysis.BT.ACK_OBD_ELD_START -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_ELD_START")
            }
            InstructionAnalysis.BT.ACK_OBD_ELD_PROCESS -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_ELD_PROCESS")
                BluetoothLESDK.replyReceivedEldData()
            }
            InstructionAnalysis.BT.ACK_OBD_ELD_FINISH -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_ELD_FINISH")
            }
            InstructionAnalysis.BT.ACK_OBD_QUERY_HISTORY_PROGRESS -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_QUERY_HISTORY_PROGRESS")
                BluetoothLESDK.replyReceivedHistoryProgress()
                // Handle business logic
            }
            InstructionAnalysis.BT.ACK_OBD_QUERY_HISTORY_DATA -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_QUERY_HISTORY_DATA")
            }
            InstructionAnalysis.BT.ACK_OBD_STOP_HISTORY_DATA_QUERY -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_STOP_HISTORY_DATA_QUERY")
            }
            InstructionAnalysis.BT.ACK_OBD_QUERY_TERMINAL_INFO -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_QUERY_TERMINAL_INFO")
            }
            InstructionAnalysis.BT.ACK_OBD_QUERY_DATA_ITEM_CONFIG -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_QUERY_DATA_ITEM_CONFIG")
            }
            InstructionAnalysis.BT.ACK_OBD_SET_DATA_ITEM_CONFIG -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_SET_DATA_ITEM_CONFIG")
            }
            InstructionAnalysis.BT.ACK_OBD_SEND_CUSTOM_COMMAND -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_SEND_CUSTOM_COMMAND")
            }
            InstructionAnalysis.BT.ACK_OBD_CUSTOM_COMMAND_REPLY -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_CUSTOM_COMMAND_REPLY")
                BluetoothLESDK.replyReceiveCustomCommandReply()
            }
            InstructionAnalysis.BT.ACK_OBD_SAVE_DRIVER_AUTH_INFO -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_SAVE_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_OBD_READ_DRIVER_AUTH_INFO -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_ALM_REPORT -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_ASCII_COMMANDS -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_BLE_NAME_MODIFICATION -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_CONNECTED -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_DASHBOARD_DATA_READING -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_DASHBOARD_DATA_REPORTING -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_DPF_REGENERATION_CHECK_RESULT -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_DPF_REGENERATION_RESULT -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_DPF_REGENERATION_UPLOAD_RESULT -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_FIND_CAR -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_FIND_CAR_ALARM -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_FIND_PHONE_ALARM -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_HID_RANGE_ADJUSTMENT -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_HID_SWITCH_STATUS -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_IMEI_VALIDATION -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_IMEI_WRITING -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_IMMOBILIZER_STATUS -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_INFO_REPORT -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_IN_HID_RANGE -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_LOCK_CAR -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_OBD_CLEAR_FAULT_CODE -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_OBD_COLLECT_FINISH -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_OBD_COLLECT_READY -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_OBD_COLLECT_START -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_OBD_COLLECT_TRANSMIT -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_OBD_UPDATE_PROGRESS -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_ONE_CLICK_STATUS_CHECK -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_OPEN_SEAT_BUCKET -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_OTA_FILE_FRAGMENTATION -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_OTA_FINISH -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_OTA_START -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_OUT_HID_RANGE -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_PAIR_ACCESS -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_PAIR_RESULT -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_QUERY_ALARM_SOUND_SWITCH -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_QUERY_POWER_SOUND_SWITCH -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_QUERY_VIBRATION_ALARM_PARAMETERS -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_SELF_CHECK_REPORT -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_SETTING_ALARM_SOUND_SWITCH -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_SETTING_POWER_SOUND_SWITCH -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_SETTING_USER_TYPE -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_SETTING_VIBRATION_ALARM_PARAMETERS -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_SYS_INFO_REPORT -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_TROUBLE_CODE -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_TURN_OFF_CAR -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_TURN_ON_CAR -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_UNDEFINED -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_UNLOCK_CAR -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_VALIDITY_PERIOD_HID -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_VERSION_NUMBER -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_VERSION_REPORT -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.ACK_VERSION_TYPE -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.PACKET_HEADER_ADV_DATA -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.PACKET_HEADER_ALM_DATA -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.PACKET_HEADER_ALM_REPORT -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.PACKET_HEADER_AT_DATA -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.PACKET_HEADER_CID_DATA -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.PACKET_HEADER_CMD_DATA -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.PACKET_HEADER_FIND_PHONE -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.PACKET_HEADER_HID_VALID_DATA -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.PACKET_HEADER_IMEI_DATA -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.PACKET_HEADER_INFO_REPORT -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.PACKET_HEADER_KEY_DATA -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.PACKET_HEADER_MTU_DATA -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.PACKET_HEADER_OBD -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.PACKET_HEADER_OTA_FILE -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.PACKET_HEADER_RPT_DATA -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.PACKET_HEADER_STA_DATA -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.PACKET_HEADER_SYSINFO_REPORT -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.PACKET_HEADER_VER_DATA -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.PACKET_HEADER_VER_QUERY_DATA -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.BT.PACKET_HEADER_VER_TYPE_DATA -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }

            ///ET
            InstructionAnalysis.ET.ACK_CLOSE_SEAT_BUCKET -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.ET.ACK_FIND_CAR -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.ET.ACK_HID_OFF -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.ET.ACK_HID_ON -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.ET.ACK_HID_STATUS -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.ET.ACK_IMMOBILIZER_STATUS -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.ET.ACK_LOCK_CAR -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.ET.ACK_OPEN_SEAT_BUCKET -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.ET.ACK_TROUBLE_CODE -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.ET.ACK_TURN_OFF_CAR -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.ET.ACK_TURN_ON_CAR -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.ET.ACK_UNDEFINED -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.ET.ACK_UNLOCK_CAR -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_UNLOCK_CAR")
            }
            InstructionAnalysis.ET.ACK_VEHICLE_POWER_STATUS -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }

            ///IOT
            InstructionAnalysis.IOT.ACK_COMMAND_PACKET_LOSS -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.IOT.ACK_COMMAND_RESPONSE -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.IOT.ACK_LOG_RESPONSE -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.IOT.ACK_OTA_FILE_ALLOW_RECEPTION -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.IOT.ACK_OTA_FILE_FRAGMENTATION -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.IOT.ACK_OTA_FILE_RECEPTION_RESULT -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.IOT.ACK_TROUBLE_CODE -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.IOT.ACK_UNDEFINED -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.IOT.PACKET_HEADER_CMD -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.IOT.PACKET_HEADER_FILE -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.IOT.PACKET_HEADER_LOG -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }

            ///NF
            InstructionAnalysis.NF.ACK_FIND_CAR -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.NF.ACK_LOCK_CAR -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.NF.ACK_OPEN_SEAT_BUCKET -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.NF.ACK_TURN_OFF_CAR -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.NF.ACK_TURN_ON_CAR -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.NF.ACK_UNDEFINED -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            InstructionAnalysis.NF.ACK_UNLOCK_CAR -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> ACK_OBD_READ_DRIVER_AUTH_INFO")
            }
            else -> {
                Log.d("JIMI_IOT_MSIL", "$ON_DATA_ANALYZE :=> else ${data.ack}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val builder = BluetoothConfig.Builder()
        builder.setProtocol(EtProtocol())
        builder.setNeedNegotiationMTU(517)
        builder.setNeedFilterDevice(true)
        val config = builder.build()
        BluetoothLESDK.init(this, config, true)
        BluetoothLESDK.setDebug(boolean = true)

        BluetoothLESDK.addOnBleGattCallbackListener(onGattCallback)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ScanScreen(
                        modifier = Modifier.padding(innerPadding),
                        onPermissionButtonClick = { checkAndRequestPermissions() },
                        onScan = { onScan() },
                        startCar = { startCar() },
                        stopCar = { stopCar() },
                        onLock = { onLock() },
                        onUnLock = { onUnLock() },
                        sendUTCTime = { sendUTCTime() },
                    )
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val needed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) permissionsHigh else permissionsLow
        val denied = needed.filter {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_DENIED
        }

        if (denied.isEmpty()) {
        } else {
            permissionLauncher.launch(denied.toTypedArray())
        }
    }

    private fun onScan() {
        BluetoothLESDK.setOnScanCallbackListener(object : OnBluetoothScanCallback {
            override fun onScan(result: ScanResult) {
                val device = result.device
                val address = device.address

                if (address == mac) {
                    Log.d(TAG, "onScan")
                    BluetoothLESDK.connect(address, "865958070000552", true)
                }
            }

            override fun onScanStop() {
                Log.d(TAG, "onScan => onScanStop")
            }

            override fun onScanFinish() {
                Log.d(TAG, "onScan => onScanFinish")
            }
        })

        BluetoothLESDK.setNeedFilterDevice(true)
        BluetoothLESDK.startScan(30000)
    }

    private fun startCar() {
        var value = BluetoothLESDK.startCar();
        Log.d(TAG, "startCar $value")
    }

    private fun stopCar() {
        var value = BluetoothLESDK.shutDown();
        Log.d(TAG, "startCar $value")
    }

    private fun onLock() {
        var value = BluetoothLESDK.lock();
        Log.d(TAG, "onLock $value")
    }

    private fun onUnLock() {
        var value = BluetoothLESDK.unlock();
        Log.d(TAG, "onUnLock $value")
    }

    private fun sendUTCTime() {
        BluetoothLESDK.sendUTCTime();
        Log.d(TAG, "sendUTCTime")
    }
}

@Composable
fun ScanScreen(
    modifier: Modifier = Modifier,
    onPermissionButtonClick: () -> Unit,
    onScan: () -> Unit,
    startCar: () -> Unit,
    stopCar: () -> Unit,
    onLock: () -> Unit,
    onUnLock: () -> Unit,
    sendUTCTime: () -> Unit,
    ) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { onPermissionButtonClick() }) {
            Text(text = "Request Permission")
        }
        Button(onClick = { onScan() }) {
            Text(text = "Start Scan")
        }
        Button(onClick = { startCar() }) {
            Text(text = "Turn On")
        }
        Button(onClick = { stopCar() }) {
            Text(text = "Turn Off")
        }
        Button(onClick = { onLock() }) {
            Text(text = "Lock")
        }
        Button(onClick = { onUnLock() }) {
            Text(text = "UnLock")
        }
        Button(onClick = { sendUTCTime() }) {
            Text(text = "sendUTCTime")
        }
    }
}