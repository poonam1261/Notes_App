package com.example.myapplication;

import static com.google.firebase.database.Query.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;


import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;


import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;


public class SearchActivity extends AppCompatActivity {

    SearchView searchView;
    RecyclerView searchRecyclerView;
    NoteAdapter searchAdapter;

    private final Handler handler = new Handler();
    private final Runnable searchRunnable = new Runnable() {
        @Override
        public void run() {
            // This method triggers the search after the user pauses typing
            performSearch(searchView.getQuery().toString());
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        searchView = findViewById(R.id.search_view);

        searchRecyclerView = findViewById(R.id.search_recycler_view);

        setupSearchRecyclerView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {


                return false;
            }
        });
    }

    void setupSearchRecyclerView() {

        com.google.firebase.firestore.Query query = Utility.getCollectionReferenceForNotes().orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING);
        FirestoreRecyclerOptions<Note> options = new FirestoreRecyclerOptions.Builder<Note>().setQuery(query,Note.class).build();

        searchRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchAdapter = new NoteAdapter(options, this);
        searchRecyclerView.setAdapter(searchAdapter);

        searchAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                checkIfEmpty();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                checkIfEmpty();
            }

            void checkIfEmpty() {
                if (searchAdapter.getItemCount() == 0) {
                    // No items found
                    Utility.showToast(SearchActivity.this, "No results found for search query.");
                }
            }
        });
    }


    void performSearch(String searchText) {

        com.google.firebase.firestore.Query query;
        if (TextUtils.isEmpty(searchText)) {
          query = Utility.getCollectionReferenceForNotes().orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING);
        } else {
           query = Utility.getCollectionReferenceForNotes().orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING).whereEqualTo("title", searchText);
            // Update with your search field
        }

        FirestoreRecyclerOptions<Note> options = new FirestoreRecyclerOptions.Builder<Note>().setQuery(query, Note.class).setLifecycleOwner(this).build();
        searchAdapter.updateOptions(options);
        //Log.d("SearchActivity", "Adapter options updated");


    }

    @Override
    protected void onStart() {
        super.onStart();
        searchAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        searchAdapter.stopListening();
    }
}