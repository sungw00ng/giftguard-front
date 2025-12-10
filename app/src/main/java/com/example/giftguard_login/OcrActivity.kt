package com.example.giftguard_login

import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.giftguard_login.databinding.ActivityOcrBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions

class OcrActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOcrBinding
    private var pickedUri: Uri? = null
    private var lastRecognizedText: String = ""

    // 사진 선택 런처
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedUri = uri
            binding.imagePreview.setImageURI(uri)
            binding.tvResult.text = ""
            lastRecognizedText = ""
            setActionButtonsEnabled(false)
        } else {
            Toast.makeText(this, "이미지를 선택하지 않았어", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOcrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 👉 저장/목록 관련 버튼/리스트는 현재 기능 안 쓰니까 숨김 처리
        binding.btnSaveOcr.visibility = View.GONE
        binding.btnViewSaved.visibility = View.GONE
        binding.recyclerGifticons.visibility = View.GONE

        setActionButtonsEnabled(false)

        // 이미지 선택
        binding.btnPick.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // OCR 실행
        binding.btnRecognize.setOnClickListener {
            val uri = pickedUri ?: run {
                Toast.makeText(this, "먼저 이미지를 선택해줘", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            runOcr(uri)
        }

        // 결과 텍스트 복사
        binding.btnCopy.setOnClickListener {
            val text = binding.tvResult.text?.toString().orEmpty()
            if (text.isBlank()) {
                Toast.makeText(this, "복사할 텍스트가 없어", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val cm = getSystemService(ClipboardManager::class.java)
            cm.setPrimaryClip(ClipData.newPlainText("OCR", text))
            Toast.makeText(this, "복사했어", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setActionButtonsEnabled(enabled: Boolean) {
        binding.btnCopy.isEnabled = enabled
    }

    // ===== OCR =====
    private fun runOcr(uri: Uri) {
        try {
            val image = InputImage.fromFilePath(this, uri)
            val recognizer = TextRecognition.getClient(
                KoreanTextRecognizerOptions.Builder().build()
            )
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    lastRecognizedText = visionText.text.orEmpty()
                    binding.tvResult.text = lastRecognizedText
                    setActionButtonsEnabled(lastRecognizedText.isNotBlank())
                }
                .addOnFailureListener { e ->
                    lastRecognizedText = ""
                    binding.tvResult.text = ""
                    setActionButtonsEnabled(false)
                    Toast.makeText(this, "인식 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(this, "이미지 로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
