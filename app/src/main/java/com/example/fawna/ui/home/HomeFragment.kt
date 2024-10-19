package com.example.fawna.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.fawna.databinding.FragmentHomeBinding

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
        trinketAdapter = TrinketAdapter()
        binding.gridTrinkets.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = trinketAdapter
        }
    }

    private fun setupConnectButton() {
        binding.buttonConnect.setOnClickListener {
            // TODO: Implement connect functionality
            Toast.makeText(context, "Connect button clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        homeViewModel.trinkets.observe(viewLifecycleOwner) { trinkets ->
            trinketAdapter.submitList(trinkets)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}