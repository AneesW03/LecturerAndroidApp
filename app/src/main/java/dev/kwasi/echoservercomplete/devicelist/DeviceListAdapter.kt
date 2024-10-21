package dev.kwasi.echoservercomplete.devicelist

import android.annotation.SuppressLint
import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import dev.kwasi.echoservercomplete.R
import android.widget.TextView
import android.widget.Button



class DeviceListAdapter() : RecyclerView.Adapter<DeviceListAdapter.ViewHolder>() {
    private val connectedDevicesList: MutableList<WifiP2pDevice> = mutableListOf()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceNameTextView: TextView = itemView.findViewById(R.id.studentIDTextView)
       // val sendMessageButton: Button = itemView.findViewById(R.id.messageButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.device_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = connectedDevicesList[position]
        holder.deviceNameTextView.text = device.deviceName
        //holder.sendMessageButton.setOnClickListener {
            //onSendMessage(device)
       // }
    }

    override fun getItemCount(): Int {
        return connectedDevicesList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateConnectedDevices(newDevices: Collection<WifiP2pDevice>){
        connectedDevicesList.clear()
        connectedDevicesList.addAll(newDevices)
        notifyDataSetChanged()
    }
}