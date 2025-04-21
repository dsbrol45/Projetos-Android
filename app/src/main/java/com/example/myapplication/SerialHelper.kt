package com.example.myapplication


import android.content.Context

import android.hardware.usb.UsbDevice

import android.hardware.usb.UsbDeviceConnection

import android.hardware.usb.UsbManager

import com.felhr.usbserial.UsbSerialDevice

import com.felhr.usbserial.UsbSerialInterface

import kotlinx.coroutines.CoroutineScope

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.launch

import kotlinx.coroutines.withContext



class SerialHelper(

    private val context: Context,

    private val onConnectionStatusChanged: (Boolean) -> Unit,

    private val onDataReceived: (String) -> Unit

) {

    private var usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private var serialPort: UsbSerialDevice? = null

    private var connection: UsbDeviceConnection? = null

    private var connected = false



    fun connect(device: UsbDevice) {

        CoroutineScope(Dispatchers.IO).launch {

            try {

                connection = usbManager.openDevice(device)

                serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection).apply {

                    open()

                    setBaudRate(115200)

                    setDataBits(UsbSerialInterface.DATA_BITS_8)

                    setStopBits(UsbSerialInterface.STOP_BITS_1)

                    setParity(UsbSerialInterface.PARITY_NONE)

                    setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)



                    read { data ->

                        val message = String(data, Charsets.UTF_8)

                        onDataReceived(message)

                    }



                    serialPort?.setDTR(true)

                    serialPort?.setRTS(true)

                }



                connected = true

                withContext(Dispatchers.Main) {

                    onConnectionStatusChanged(true)

                }

            } catch (e: Exception) {

                disconnect()

                withContext(Dispatchers.Main) {

                    onDataReceived("Erro na conexão: ${e.message}")

                }

            }

        }

    }



    fun disconnect() {

        serialPort?.close()

        connection?.close()

        serialPort = null

        connection = null

        connected = false

        onConnectionStatusChanged(false)

    }



    fun sendMessage(message: String) {

        if (connected) {

            CoroutineScope(Dispatchers.IO).launch {

                try {

                    serialPort?.write(message.toByteArray())

                } catch (e: Exception) {

                    withContext(Dispatchers.Main) {

                        onDataReceived("Erro ao enviar: ${e.message}")

                    }

                }

            }

        }

    }



    fun isConnected(): Boolean = connected

}