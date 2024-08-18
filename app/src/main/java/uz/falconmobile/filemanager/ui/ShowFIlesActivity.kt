package uz.falconmobile.filemanager.ui

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import uz.falconmobile.filemanager.BuildConfig
import uz.falconmobile.filemanager.R
import uz.falconmobile.filemanager.adapters.FileListAdapter
import uz.falconmobile.filemanager.databinding.ActivityShowFilesBinding
import uz.falconmobile.filemanager.models.FileItem
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class ShowFIlesActivity : AppCompatActivity() {


    private var copiedFiles: List<File>? = null
    private val REQUEST_CODE_PICK_DESTINATION_DIRECTORY = 101
    private val REQUEST_CODE_STORAGE_PERMISSION = 100
    private val REQUEST_CODE_PICK_DIRECTORY = 1001
    private lateinit var btnCancelSelection: Button
    private lateinit var rvFileList: RecyclerView
    private lateinit var adapter: FileListAdapter
    private lateinit var btnDelete: Button
    private lateinit var btnRename: Button
    private lateinit var btnDestails: Button
//    private lateinit var cardview: CardView
    private lateinit var viewall: TextView
    private lateinit var currentDirectory: File
    private val rootDirectory = Environment.getExternalStorageDirectory()

private lateinit var binding:ActivityShowFilesBinding
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityShowFilesBinding.inflate(layoutInflater)
        setContentView(binding.root)



        btnCancelSelection = binding.btnCancelSelection
        btnDestails = binding.btnDestails
        btnDelete = binding.btnDelete
        btnRename = binding.btnRename
        rvFileList = binding.rvFileList
//        cardview = findViewById(R.id.cardView)
        viewall =binding.tvViewAll

        adapter = FileListAdapter(this,
            onItemClick = { fileItem ->
                if (fileItem.isDirectory) {
                    navigateToDirectory(fileItem)
                } else {
                    openFile(fileItem.file)
                }
            },
            onDeleteClick = {
                // Handle deletion completion here if needed
            },
            onRenameClick = { fileItem, newName ->
                val oldFile = File(fileItem.path)
                val newFile = File(oldFile.parent, newName)

                try {
                    if (oldFile.exists() && oldFile.isFile && !newFile.exists()) {
                        val success = oldFile.renameTo(newFile)
                        if (success) {
                            val message =
                                "File renamed successfully: ${oldFile.path} to ${newFile.path}"
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        } else {
                            val errorMessage = "Failed to rename: Unknown error"
                            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val errorMessage = when {
                            !oldFile.exists() -> "Failed to rename: File does not exist"
                            !oldFile.isFile -> "Failed to rename: Not a file"
                            newFile.exists() -> "Failed to rename: File with the new name already exists"
                            else -> "Failed to rename: Unknown error"
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("FileRename", "Failed to rename: ${e.message}", e)
                    Toast.makeText(this, "Failed to rename: Unknown error", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        )


//        displayStorageInfo()
//        displayFileSizes()









        rvFileList.layoutManager = LinearLayoutManager(this)
        rvFileList.adapter = adapter

        currentDirectory = rootDirectory
        listFiles(currentDirectory)

        btnCancelSelection.setOnClickListener {
            adapter.cancelSelectionMode()
        }
        btnRename.setOnClickListener {
            adapter.renameSelectedItems()
        }
        btnDestails.setOnClickListener {
            adapter.showDetailsForSelectedItem()
        }
        btnDelete.setOnClickListener {
            adapter.deleteSelectedItems()
        }

        val btnSelectAll: Button = findViewById(R.id.btnSelectALl)
        btnSelectAll.setOnClickListener {
            adapter.selectAllItems()
        }
//
//        viewall.setOnClickListener {
//            val ll = findViewById<LinearLayout>(R.id.ll)
//            if (cardview.visibility == View.VISIBLE) {
//                cardview.visibility = View.GONE
//                ll.visibility = View.VISIBLE
//                viewall.setText("Go to dashboard")
//            } else {
//                cardview.visibility = View.VISIBLE
//                ll.visibility = View.GONE
//                viewall.setText("view all")
//            }
//        }


        val btnMove: Button = findViewById(R.id.btnMove)
        btnMove.setOnClickListener {
            pickDestinationDirectory()
        }

//
//        val progressBar: ProgressBar = binding.progressBar
//        val path = Environment.getExternalStorageDirectory()
//        val stat = StatFs(path.path)
//        val totalBlocks = stat.blockCountLong
//        val availableBlocks = stat.availableBlocksLong
//
//        val percentage = 100 - ((availableBlocks.toFloat() / totalBlocks.toFloat()) * 100)
//
//// Set initial progress to 0
//        progressBar.progress = 0

// Animate progress from 0 to final percentage
//        val animator = ObjectAnimator.ofInt(progressBar, "progress", 0, percentage.toInt())
//        animator.duration = 2000 // You can adjust the duration as needed
//        animator.start()



    }


    fun onCopyButtonClick(view: View) {
        val selectedFiles = adapter.getSelectedItems().map { it.file }
        copyFiles(selectedFiles)
        copiedFiles = adapter.getSelectedItems().map { it.file }
    }

    fun onPasteButtonClick(view: View) {
        if (copiedFiles != null) {
            pickDestinationDirectory()
        } else {
            Toast.makeText(this, "No files copied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_DESTINATION_DIRECTORY && resultCode == Activity.RESULT_OK) {
            handleDestinationDirectoryResult(data)
        } else if (requestCode == REQUEST_CODE_PICK_DIRECTORY && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            // Convert URI to File or obtain directory path from the URI
            // Pass the selected directory to the moveSelectedItems or pasteSelectedItems method in your adapter
            val selectedDirectory = uri?.let { documentUri ->
                DocumentFile.fromTreeUri(this, documentUri)?.let { documentFile ->
                    File(documentFile.uri.path!!)
                }
            }
            selectedDirectory?.let {
                // Pass the selected directory to the adapter to move or paste the selected items
                adapter.moveSelectedItems(it) // or fileAdapter.pasteSelectedItems(it)
            }
        }
    }


    private fun handleDestinationDirectoryResult(data: Intent?) {
        val uri = data?.data
        if (uri != null) {
            val destinationDirectory = DocumentFile.fromTreeUri(this, uri)?.let { documentFile ->
                // Use content resolver to resolve DocumentFile to File
                File(documentFile.uri.path)
            }
            if (destinationDirectory != null) {
                copiedFiles?.let { files ->
                    pasteFiles(files, destinationDirectory)
                }
            } else {
                Toast.makeText(this, "Failed to access selected directory", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            Toast.makeText(this, "No directory selected", Toast.LENGTH_SHORT).show()
        }
    }


    private fun copyFiles(filesToCopy: List<File>) {
        val destinationDirectory = getDestinationDirectory()
        filesToCopy.forEach { file ->
            val destinationFile = File(destinationDirectory, file.name)
            try {
                FileInputStream(file).use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to copy files", Toast.LENGTH_SHORT).show()
                return
            }
        }
        Toast.makeText(this, "Files copied successfully", Toast.LENGTH_SHORT).show()
    }

    private fun pasteFiles(filesToPaste: List<File>, destinationDirectory: File) {
        try {
            filesToPaste.forEach { file ->
                val destinationFile = File(destinationDirectory, file.name)
                FileInputStream(file).use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            Toast.makeText(this, "Files pasted successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to paste files: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun navigateToDirectory(directory: FileItem) {
        currentDirectory = File(directory.path)
        listFiles(currentDirectory)
    }

    private fun listFiles(directory: File) {
        val files = directory.listFiles()
        val fileItems = files?.map { file ->
            FileItem(file.name, file.absolutePath, file.isDirectory, file)
        }
        adapter.submitList(fileItems)
    }

    private fun openFile(file: File) {
        val uri = FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.provider", file)
        val mimeType = getMimeType(file)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, mimeType)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(intent)
    }

    private fun getMimeType(file: File): String? {
        val extension = file.extension.toLowerCase()
        return when (extension) {
            "jpg", "jpeg", "png", "gif" -> "image/*"
            "mp3", "wav", "ogg" -> "audio/*"
            "mp4", "3gp", "avi" -> "video/*"
            else -> "*/*"
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissions granted, proceed with your app logic
            } else {
                // Permissions denied, show a message or handle the situation accordingly
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private var backPressedOnce = false

    override fun onBackPressed() {
        if (adapter.isSelectionModeActive()) {
            adapter.cancelSelectionMode()
            return
        }

        if (currentDirectory != rootDirectory) {
            currentDirectory = currentDirectory.parentFile ?: rootDirectory
            listFiles(currentDirectory)
            return
        }

        if (backPressedOnce) {
            super.onBackPressed()
            return
        }

        this.backPressedOnce = true
        Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()

        android.os.Handler().postDelayed({
            backPressedOnce = false
        }, 2000) // Time window in milliseconds to press back button again to exit
    }


    private fun getDestinationDirectory(): File {
        return Environment.getExternalStorageDirectory()
    }

    private fun pickDestinationDirectory() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, REQUEST_CODE_PICK_DIRECTORY)
    }


}