package com.telemedicine.indihealth.ui.fragment.consultation.assessment.detail

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.telemedicine.indihealth.R
import com.telemedicine.indihealth.base.BaseFragment
import com.telemedicine.indihealth.binding.checkIfTIETIsEmpty
import com.telemedicine.indihealth.databinding.LayoutConsultationAssessmentBinding
import com.telemedicine.indihealth.helper.Event
import com.telemedicine.indihealth.ui.dialog.DialogNotification
import com.telemedicine.indihealth.ui.fragment.profile.patient.edit.ProfileEditViewModel
import kotlinx.android.synthetic.main.layout_consultation_assessment.*
import pl.aprilapps.easyphotopicker.DefaultCallback
import pl.aprilapps.easyphotopicker.EasyImage
import pl.aprilapps.easyphotopicker.MediaFile
import pl.aprilapps.easyphotopicker.MediaSource
import timber.log.Timber
import java.util.*

class ConsultationAssessmentFragment : BaseFragment() {
    private val editViewModel: ProfileEditViewModel by activityViewModels()
    private lateinit var mBinding: LayoutConsultationAssessmentBinding
    private lateinit var navController: NavController
    private val args: ConsultationAssessmentFragmentArgs? by navArgs()
    private lateinit var imageAdapter: AdditionImageAdapter
    private lateinit var easyImage: EasyImage

    val photo: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }
    val isEditIn: MutableLiveData<Event<String>> by lazy {
        MutableLiveData<Event<String>>()
    }

    private fun setEditInStatus(string: String) {
        isEditIn.postValue(Event(string))
    }

    companion object {
        fun startFragment(view: View) {
            view.findNavController()
                .navigate(R.id.action_mainFragment_to_consultationAssessmentFragment)
        }

        fun startFragmentDirection(fragment: Fragment) {
//            val action =
//                ConsultationPaymentFragmentDirections
//                    .actionConsultationPaymentFragmentToConsultationRegistrationFragment()
            NavHostFragment.findNavController(fragment)
                .navigate(R.id.action_consultationPaymentFragment_to_consultationRegistrationFragment)
        }

        fun pendingIntent(context: Context): PendingIntent {
            return NavDeepLinkBuilder(context)
                .setGraph(R.navigation.navigation_main)
                .setDestination(R.id.consultationAssessmentFragment)
                .createPendingIntent()
        }
    }

    private val viewModel: ConsultationAssessmentViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding = LayoutConsultationAssessmentBinding.inflate(inflater, container, false)
        viewModel.images.clear()
        return mBinding
            .apply {
                lifecycleOwner = viewLifecycleOwner
                vm = viewModel
            }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)


        imageAdapter = AdditionImageAdapter(requireContext())
        mBinding.rvImages.adapter = imageAdapter
        mBinding.rvImages.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        setObservableValue()
        setOnClickListener()
    }

    override fun onResume() {
        super.onResume()
        viewModel.initSchedule()
    }

    private fun setObservableValue() {
        viewModel.apply {
            initAssessment(args?.idJadwalKonsultasi!!)

            /*scheduleList.observe(viewLifecycleOwner, {
                if (it.isNotEmpty()) {
                    viewModel.initAssessment()
                }
            })*/
            assessment.observe(viewLifecycleOwner) {
                imageAdapter.submitList(viewModel.images)
            }
            isLoading.observe(viewLifecycleOwner, {
                Timber.d("isLoading = ${it.peekContent()}")
                loadingValidation(it, requireContext())
            })
            responseStatus.observe(viewLifecycleOwner, {
                Timber.d("responseStatus: ${it.peekContent()}")
                it.getContentIfNotHandled()?.apply {
                    when (this.getValue("status").toString()) {
                        "success" -> {
                            val dialog = DialogNotification.newInstance(
                                this.getValue("status").toString(),
                                "Pengisian Assessment Berhasil",
                                ""
                            )
                            dialog?.show(childFragmentManager, "")
                            dialog?.apply {
                                onConfirmClicked = {
                                    requireActivity().onBackPressed()
                                }
                            }
                        }
                        else -> {
                            DialogNotification.newInstance(
                                this.getValue("status").toString(),
                                "Pengisian Assessment Gagal",
                                this.getValue("msg").toString()
                            )?.show(childFragmentManager, "")
                        }
                    }
                }
            })
        }

    }

    private fun setOnClickListener() {
        mBinding.consultationAssessmentToolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }
        button.setOnClickListener {
            navController.navigate(R.id.action_consultationAssessmentFragment_to_consultationRegistrationFragment)
        }
        consultation_assessment_btn_send.setOnClickListener {
            if (validate()) {
                viewModel.onAssessmentClicked()
                viewModel.onOpenImage = false
            }
        }

        mBinding.btnSelectImage.setOnClickListener {
            easyImage = EasyImage.Builder(requireContext())
                .allowMultiple(true)
                .setChooserTitle("Pilih Gambar")
                .build()
            viewModel.onOpenImage = true
            easyImage.openChooser(this)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        easyImage.handleActivityResult(requestCode, resultCode, data, requireActivity(), object : DefaultCallback() {
            override fun onMediaFilesPicked(imageFiles: Array<MediaFile>, source: MediaSource) {
                viewModel.images.clear()
                imageFiles.forEachIndexed { index, mediaFile ->
                    if (mediaFile.file.length() > 3 * 1024 * 1024) {
                        Toast.makeText(requireContext(), "Ukuran file ke-${index+1} diatas 3MB", Toast.LENGTH_SHORT).show()
                        return@forEachIndexed
                    }
                    if (index < 3) {
                        Timber.d("File Size: ${mediaFile.file.length() / 1024}KB -> ${mediaFile.file.name}")
                        viewModel.addImage(mediaFile.file.toUri())
                    }
                }
                imageAdapter.submitList(viewModel.images)
                if (imageFiles.size > 3) {
                    Toast.makeText(requireContext(), "Maksimal hanya 3 gambar", Toast.LENGTH_SHORT).show()
                }
                Timber.d("Image length: ${imageAdapter.itemCount}")
                imageAdapter.notifyDataSetChanged()
            }

        })
    }



//        consultation_radiobutton_penyakit.setOnCheckedChangeListener { group, checkedId ->
//            if (checkedId == R.id.consultation_assessment_radio_6) {
//                consultation_assessment_til_penyakit_text.isEnabled = true
//            }
//            else {
//                consultation_assessment_til_penyakit_text.setText("Tidak Ada")
//                consultation_assessment_til_penyakit_text.isEnabled = false
//            }
//        }
//
//        consultation_radiobutton_alergi.setOnCheckedChangeListener { group, checkedId ->
//            if (checkedId == R.id.consultation_assessment_radio_7) {
//                consultation_assessment_til_alergi_text.isEnabled = true
//            }
//            else {
//                consultation_assessment_til_alergi_text.setText("Tidak Ada")
//                consultation_assessment_til_alergi_text.isEnabled = false
//            }
//        }


//        mBinding.cvImagePicker.setOnClickListener {
//            ImagePicker.with(this)
//                .cropSquare()
//                .compress(3000)
//                .maxResultSize(620, 620)
//                .start()
//        }
    //}

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        when (resultCode) {
//            Activity.RESULT_OK -> {
//                //Image Uri will not be null for RESULT_OK
//                val fileUri = data?.data
//
//                //You can get File object from intent
//                val file: File = ImagePicker.getFile(data)!!
//                Timber.d("${file.length()/1024}")
//                val fileSize = file.sizeInKb
//                Timber.d("Size ini tuh %s", fileSize)
//                if(fileSize > 3072){
//                    Toast.makeText(requireContext(), "Maksimal file untuk diupload 3MB", Toast.LENGTH_SHORT).show()
//                }else {
//                    setImage(fileUri)
//                    editViewModel.setPhoto(file)
//                }
//
//            }
//            ImagePicker.RESULT_ERROR -> {
//                Toast.makeText(requireContext(), ImagePicker.getError(data), Toast.LENGTH_SHORT)
//                    .show()
//                setImageEmpty()
//            }
//            else -> {
//                Toast.makeText(requireContext(), "Cari foto dibatalkan", Toast.LENGTH_SHORT).show()
//                setImageEmpty()
//            }
//        }
//    }

//    private fun setImage(uri: Uri?) {
//        GlideApp
//            .with(consultation_payment_detail_iv_photo)
//            .load(uri)
//            .placeholder(R.drawable.placeholder_rectangle)
//            .into(mBinding.noImage)
        //consultation_payment_detail_iv_photo.visibility = View.VISIBLE
//        consultation_payment_detail_ll_pick_photo.visibility = View.GONE
//        consultation_payment_detail_btn_confirm.isEnabled = true
  //  }

//    private fun setImageEmpty() {
//        consultation_payment_detail_iv_photo.visibility = View.GONE
//        consultation_payment_detail_ll_pick_photo.visibility = View.VISIBLE
//        consultation_payment_detail_btn_confirm.isEnabled = false
   // }

//    val File.size get() = if (!exists()) 0.0 else length().toDouble()
//    val File.sizeInKb get() = size / 1024
//
//    var fileToUpload: MultipartBody.Part? = null
//
//    fun setPhoto(file: File) {
//        // Parsing any Media type file
//        val requestBody: RequestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
//        fileToUpload =
//            MultipartBody.Part.createFormData("foto", file.name, requestBody)
//        Timber.d("requestBody = $fileToUpload")
//    }

    private fun validate(): Boolean {
        if (consultation_assessment_til_alergi_text.text!!.isEmpty()){
            consultation_assessment_til_alergi_text.setText("")
        }
        if (consultation_assessment_til_penyakit_text.text!!.isEmpty()){
            consultation_assessment_til_penyakit_text.setText("")
        }

//        if (fileToUpload == null) {
//            if (photo.value == "") {
//                setEditInStatus("tolong pilih foto profil")
//            }else{
//                Timber.d("file to upload = $fileToUpload")
//            }
//        }
        if (checkIfTIETIsEmpty(
                consultation_assessment_til_keluhan,
                viewModel.assessment.value?.keluhan?.isEmpty()!!
            )
       )
        { Timber.d("Assessment validate is false= ${viewModel.assessment.value}")
            return false
        } else {
            Timber.d("Assessment validate is true= ${viewModel.assessment.value}")
            return true
        }
    }


// Validasi sebelumnya
//            ||checkIfTIETIsEmpty(
//                consultation_assessment_til_alergi,
//                viewModel.assessment.value?.riwayat_alergi?.isEmpty()!!
//            )||checkIfTIETIsEmpty(
//                consultation_assessment_til_penyakit,
//                viewModel.assessment.value?.riwayat_penyakit?.isEmpty()!!
//            ) || checkIfRadioButtonIsEmpty(
//                consultation_assessment_radio_1_0,
//                viewModel.assessment.value?.merokok?.isEmpty()!!
//            ) || checkIfRadioButtonIsEmpty(
//                consultation_assessment_radio_2_0,
//                viewModel.assessment.value?.alkohol?.isEmpty()!!
//            ) || checkIfRadioButtonIsEmpty(
//                consultation_assessment_radio_3_0,
//                viewModel.assessment.value?.kecelakaan?.isEmpty()!!
//            ) || checkIfRadioButtonIsEmpty(
//                consultation_assessment_radio_4_0,
//                viewModel.assessment.value?.dirawat?.isEmpty()!!
//            ) || checkIfRadioButtonIsEmpty(
//                consultation_assessment_radio_5_0,
//                viewModel.assessment.value?.operasi?.isEmpty()!!
//            )


}