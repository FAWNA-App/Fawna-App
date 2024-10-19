//package com.example.fawna.ui.search
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import android.widget.Toast
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.ViewModelProvider
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.example.fawna.BleNodeManager
//import com.example.fawna.R
//import com.example.fawna.databinding.FragmentSearchBinding
//import com.example.fawna.ui.search.SearchViewModel
//
//class DeviceListAdapter(
//    private val devices: List<Pair<String, Int>>,
//    private val onItemClicked: (String) -> Unit
//) : RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder>() {
//
//    class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//        val deviceName: TextView = view.findViewById(R.id.device_name)
//        val deviceHopCount: TextView = view.findViewById(R.id.device_hop_count)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.device_list_item, parent, false)
//        return DeviceViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
//        val (deviceId, hopCount) = devices[position]
//        holder.deviceName.text = deviceId
//        holder.deviceHopCount.text = "$hopCount hops away"
//
//        holder.itemView.setOnClickListener {
//            // Trigger the callback directly
//            onItemClicked(deviceId)
//        }
//    }
//
//    override fun getItemCount() = devices.size
//}
//
//class SearchFragment : Fragment() {
//
//    private var _binding: FragmentSearchBinding? = null
//    private val binding get() = _binding!!
//
//    private lateinit var recyclerView: RecyclerView
//    private lateinit var deviceListAdapter: DeviceListAdapter
//    private lateinit var bleNodeManager: BleNodeManager
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        val searchViewModel = ViewModelProvider(this).get(SearchViewModel::class.java)
//
//        _binding = FragmentSearchBinding.inflate(inflater, container, false)
//        val root: View = binding.root
//
//        val textView: TextView = binding.textSearch
//        searchViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }
//
//        // Initialize RecyclerView
//        recyclerView = binding.recyclerView // Ensure you have added RecyclerView in fragment_search.xml
//        recyclerView.layoutManager = LinearLayoutManager(context)
//
//        return root
//    }
//
//    override fun onStart() {
//        super.onStart()
//        bleNodeManager = BleNodeManager(requireContext())
//        bleNodeManager.start()
//        updateDeviceList()
//    }
//
//    override fun onResume() {
//        super.onResume()
//        updateDeviceList()
//    }
//
//    private fun updateDeviceList() {
//        val discoveredDevices = bleNodeManager.getDiscoveredDevices()
//        val deviceList = discoveredDevices.map { it.key to it.value }
//
//        deviceListAdapter = DeviceListAdapter(deviceList) { deviceId ->
//            // Handle the device click here instead of showing a dialog
//            Toast.makeText(requireContext(), "Clicked on $deviceId", Toast.LENGTH_SHORT).show()
//        }
//
//        recyclerView.adapter = deviceListAdapter
//    }
//
//
////    private fun showInteractionDialog(deviceId: String) {
////        val interactionFragment = InteractionFragment.newInstance(deviceId)
////        interactionFragment.show(parentFragmentManager, "InteractionDialog")
////    }
//
//    override fun onStop() {
//        super.onStop()
////        bleNodeManager.stop() // Stop BLE scanning if necessary
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}
