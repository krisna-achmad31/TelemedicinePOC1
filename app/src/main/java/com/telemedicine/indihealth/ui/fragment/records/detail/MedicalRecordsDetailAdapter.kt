package com.telemedicine.indihealth.ui.fragment.records.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.telemedicine.indihealth.R
import com.telemedicine.indihealth.databinding.ItemMedicalRecordsDetailBinding
import com.telemedicine.indihealth.model.Drug

class MedicalRecordsDetailAdapter :
    RecyclerView.Adapter<MedicalRecordsDetailAdapter.ViewHolder>() {

    private val items: MutableList<Drug> by lazy {
        mutableListOf()
    }

    val clickedItem: MutableLiveData<Drug> by lazy {
        MutableLiveData<Drug>()
    }

    class ViewHolder(val binding: ItemMedicalRecordsDetailBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding =
            DataBindingUtil.inflate<ItemMedicalRecordsDetailBinding>(
                inflater,
                R.layout.item_medical_records_detail, parent, false
            )
        return ViewHolder(
            binding
        )
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.apply {
            drug = item
            root.setOnClickListener {
                clickedItem.postValue(item)
            }
        }
    }

    fun addList(list: List<Drug>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }
}
