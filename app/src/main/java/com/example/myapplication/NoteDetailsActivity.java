package com.example.myapplication;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import android.Manifest;


public class NoteDetailsActivity extends AppCompatActivity implements ActivityResultCaller {

    private ActivityResultLauncher<String> imagePickerLauncher;

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final int PERMISSIONS_REQUEST = 3;
    EditText titleEditText,contentEditText;
    ImageButton saveNoteBtn,deleteImageBtn;
    TextView pageTitleTextView;
    String title,content,docId;
    boolean isEditMode = false;
    TextView deleteNoteTextViewBtn;
    FloatingActionButton shareNoteBtn;

    ImageView addImageBtn, noteImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_note_details);

        shareNoteBtn = findViewById(R.id.share_btn);
        titleEditText = findViewById(R.id.note_title_text);
        contentEditText = findViewById(R.id.note_content_text);
        saveNoteBtn = findViewById(R.id.save_note_btn);
        pageTitleTextView = findViewById(R.id.page_title);
        deleteNoteTextViewBtn = findViewById(R.id.delete_note_text_view_btn);
        addImageBtn = findViewById(R.id.add_image_btn);
        noteImageView = findViewById(R.id.note_image_view);
        deleteImageBtn = findViewById(R.id.delete_image_btn);

        title = getIntent().getStringExtra("title");
        content = getIntent().getStringExtra("content");
        docId = getIntent().getStringExtra("docId");
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri result) {
                        if (result != null) {
                            try {
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), result);
                                noteImageView.setImageBitmap(bitmap);
                                noteImageView.setVisibility(View.VISIBLE);
                                deleteImageBtn.setVisibility(View.VISIBLE);
                            } catch (IOException e) {
                                e.printStackTrace();


                            }
                        }
                    }
                });


        if(docId != null && !docId.isEmpty() )
        {
            isEditMode = true;
        }
        titleEditText.setText(title);
        contentEditText.setText(content);

        if(isEditMode){
            pageTitleTextView.setText("Edit your note");
            deleteNoteTextViewBtn.setVisibility(View.VISIBLE);
        }
        if (isEditMode) {
            // Assuming 'docId' is the unique identifier for the note
            Utility.getCollectionReferenceForNotes().document(docId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Note note = documentSnapshot.toObject(Note.class);
                            if (note != null && note.getImageUrl() != null && !note.getImageUrl().isEmpty()) {
                                // Load image into noteImageView using Picasso
                                Picasso.get()
                                        .load(note.getImageUrl())
                                        .into(noteImageView, new Callback() {
                                            @Override
                                            public void onSuccess() {
                                                // Image loaded successfully
                                                Log.d("PicassoSuccess", "Image loaded");
                                                noteImageView.setVisibility(View.VISIBLE);
                                                deleteImageBtn.setVisibility(View.VISIBLE);

                                            }

                                            @Override
                                            public void onError(Exception e) {
                                                // Handle error here
                                                Log.e("PicassoError", "Error loading image: " + e.getMessage());
                                                noteImageView.setVisibility(View.GONE); // Hide the ImageView on error
                                            }
                                        });
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Handle failure while fetching the note
                        Log.e("FirestoreError", "Error fetching note: " + e.getMessage());
                    });
        }

        saveNoteBtn.setOnClickListener((v)-> saveNote() );

        deleteNoteTextViewBtn.setOnClickListener((v)-> deleteNoteFromFirebase());
        shareNoteBtn.setOnClickListener((v)-> shareNote());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            addImageBtn.setOnClickListener((v) -> selectImage());
        }


        deleteImageBtn.setOnClickListener(v-> deleteImage());

    }

    private void deleteImage() {
        // Remove the image from the ImageView
        noteImageView.setImageDrawable(null);
        noteImageView.setVisibility(View.GONE);
        deleteImageBtn.setVisibility(View.GONE);

        // Remove the image from Firebase Storage if it exists
        if (docId != null && !docId.isEmpty()) {
            Utility.getCollectionReferenceForNotes().document(docId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Note note = documentSnapshot.toObject(Note.class);
                            if (note != null && note.getImageUrl() != null && !note.getImageUrl().isEmpty()) {
                                // Get the image URL and delete it from Firebase Storage
                                FirebaseStorage storage = FirebaseStorage.getInstance();
                                StorageReference imageRef = storage.getReferenceFromUrl(note.getImageUrl());
                                imageRef.delete()
                                        .addOnSuccessListener(aVoid -> {
                                            // Image deleted from Firebase Storage
                                            Log.d("DeleteImage", "Image deleted from Firebase Storage");
                                            Utility.showToast(NoteDetailsActivity.this,"Image Deleted Succefully");

                                            // Clear the image URL in Firestore
                                            note.setImageUrl(""); // Set the image URL to empty
                                            //saveNoteToFirestore(note, Utility.getCollectionReferenceForNotes().document(docId));
                                        })
                                        .addOnFailureListener(e -> {
                                            // Handle failure while deleting image
                                            Log.e("DeleteImage", "Failed to delete image from Firebase Storage: " + e.getMessage());
                                        });
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Handle failure while fetching the note
                        Log.e("DeleteImage", "Error fetching note: " + e.getMessage());
                    });
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void selectImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSIONS_REQUEST);
        } else {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imagePickerLauncher.launch("image/*");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                selectImage();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                noteImageView.setImageBitmap(bitmap);
                deleteImageBtn.setVisibility(View.VISIBLE);


            } catch (IOException e) {
                e.printStackTrace();

            }
        }
    }
    void saveNote(){
        String noteTitle = titleEditText.getText().toString();
        String noteContent = contentEditText.getText().toString();
        // Assuming noteImageView is your ImageView
        noteImageView.setDrawingCacheEnabled(true);
        noteImageView.buildDrawingCache();


        if(noteTitle == null || noteTitle.isEmpty()){
            titleEditText.setError("Title is required");
            return;
        }
        Note note ;
        note = new Note();
        note.setTitle(noteTitle);
        note.setContent(noteContent);
        note.setTimestamp(Timestamp.now());


        // Set the image data to the note object


        if (noteImageView.getDrawable() != null) {
            noteImageView.setDrawingCacheEnabled(true);
            noteImageView.buildDrawingCache();
            Bitmap imageBitmap = noteImageView.getDrawingCache();

            noteImageView.setVisibility(View.VISIBLE);

            saveNoteToFirebase(note, imageBitmap);
        } else {
            saveNoteToFirebase(note, null);
        }
    }


    void saveNoteToFirebase(Note note, Bitmap imageBitmap){
        DocumentReference documentReference = Utility.getCollectionReferenceForNotes().document(
                isEditMode ? docId : Utility.getCollectionReferenceForNotes().document().getId()
        );




        if (imageBitmap != null) {
            uploadImageToFirebaseStorage(note, imageBitmap, documentReference);
            noteImageView.setVisibility(View.VISIBLE);
            deleteImageBtn.setVisibility(View.VISIBLE);


        } else {
            // No image to upload, save note without image URL directly to Firestore
            saveNoteToFirestore(note, documentReference);
        }
    }

    void uploadImageToFirebaseStorage(Note note, Bitmap imageBitmap, DocumentReference documentReference) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        // Generate a unique filename for the image in Firebase Storage
        String imageName = "note_image_" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageRef.child("images/" + imageName);

        // Convert imageBitmap to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageData = baos.toByteArray();

        // Upload the image data to Firebase Storage
        imageRef.putBytes(imageData)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get the download URL for the uploaded image
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        // Set the image URL to the note object
                        note.setImageUrl(uri.toString());

                        Log.d("NoteImageURL", "Note's image URL: " + note.getImageUrl());
                        Utility.showToast(NoteDetailsActivity.this,"Image added successfully");
                        // Assuming you have the imageUrl from the Note object
                        String imageUrl = note.getImageUrl();

// Load image into noteImageView using Picasso
                        Picasso.get()
                                .load(imageUrl)
                                .into(noteImageView, new Callback() {
                                    @Override
                                    public void onSuccess() {
                                        // Image loaded successfully
                                        Log.e("PicassoSuccess", "  image loaded ");
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        // Handle error here
                                        Log.e("PicassoError", "Error loading image: " + e.getMessage());
                                    }
                                });


                        noteImageView.setVisibility(View.VISIBLE);

                        deleteImageBtn.setVisibility(View.VISIBLE);

                        // Save the note (with image URL) to Firestore
                        saveNoteToFirestore(note, documentReference);

                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("ImageUpload", "Image upload failed: " + e.getMessage());

                    // Handle image upload failure
            Utility.showToast(NoteDetailsActivity.this,"Image addition failed"+ e.getMessage());
                });
    }


    void saveNoteToFirestore(Note note, DocumentReference documentReference) {

        Log.d("SaveToFirestore", "Saving note to Firestore...");

        documentReference.set(note)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("SaveToFirestore", "Note added successfully to Firestore");

                        Utility.showToast(NoteDetailsActivity.this, "Note added successfully");
                        finish();
                    } else {
                        Log.e("SaveToFirestore", "Failed to add note to Firestore: " + task.getException());

                        Utility.showToast(NoteDetailsActivity.this, "Failed while adding notes");
                    }
                });
    }

    void deleteNoteFromFirebase(){
        DocumentReference documentReference;

            // update the note
            documentReference = Utility.getCollectionReferenceForNotes().document(docId);


        documentReference.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    /// note is deleted
                    Utility.showToast(NoteDetailsActivity.this,"Note deleted successfully");
                    finish();
                }else{
                    Utility.showToast(NoteDetailsActivity.this,"Failed while deleting the note");

                }
            }
        });
    }
    private void shareNote() {
        String noteTitle = titleEditText.getText().toString();
        String noteContent = contentEditText.getText().toString();

        // Combine title and content for sharing
        String shareText = "Note Title: " + noteTitle + "\n\n" + "Note Content:\n" + noteContent;

        // Create a file with .txt extension
        File noteFile = createTxtFile(shareText);

        if (noteFile != null) {
            // Get the content URI using FileProvider
            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", noteFile);

            // Create an intent to share the .txt file
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Note Shared from My App");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Start the chooser activity to let the user pick an app to share through
            startActivity(Intent.createChooser(shareIntent, "Share Note Using"));
        }
    }


    private File createTxtFile(String textToWrite) {
        try {
            File dir = new File(getFilesDir(), "shared_notes");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Create a file in the directory
            File file = new File(dir, "shared_note.txt");
            FileWriter writer = new FileWriter(file);

            // Write the text content to the file
            writer.append(textToWrite);
            writer.flush();
            writer.close();

            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


}