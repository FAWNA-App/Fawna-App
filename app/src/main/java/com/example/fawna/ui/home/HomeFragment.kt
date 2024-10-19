package com.example.fawna.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.fawna.R
import com.example.fawna.databinding.DialogTrinketDetailsBinding
import com.example.fawna.databinding.FragmentHomeBinding
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var trinketAdapter: TrinketAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupConnectButton()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        trinketAdapter = TrinketAdapter { trinket ->
            homeViewModel.selectTrinket(trinket.id)
        }
        binding.gridTrinkets.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = trinketAdapter
        }
    }

    private fun setupConnectButton() {
        binding.buttonConnect.setOnClickListener {
            Toast.makeText(context, "Connect button clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        homeViewModel.trinkets.observe(viewLifecycleOwner) { trinkets ->
            trinketAdapter.submitList(trinkets)
        }

        homeViewModel.selectedTrinket.observe(viewLifecycleOwner) { trinketDetails ->
            showTrinketDetailsPopup(trinketDetails)
        }
    }

    private fun showTrinketDetailsPopup(details: TrinketDetails) {
        val dialogBinding = DialogTrinketDetailsBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setTitle("Trinket Details")
            .setPositiveButton("OK", null)
            .create()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        with(dialogBinding) {
            trinketId.text = "ID: ${details.id}"
            trinketDateCreated.text = "Created: ${dateFormat.format(details.dateCreated)}"
            trinketQuestion.text = "Question: ${details.question}"
            trinketAnswer.text = "Answer: ${details.answer}"
            trinketTimestamp.text = "Timestamp: ${dateFormat.format(details.timestamp)}"
            trinketNotes.text = "Notes: ${details.notes}"
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}