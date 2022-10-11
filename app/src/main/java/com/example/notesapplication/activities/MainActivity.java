package com.example.notesapplication.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.example.notesapplication.R;
import com.example.notesapplication.activities.CreateNoteActivity;
import com.example.notesapplication.adapters.NotesAdapter;
import com.example.notesapplication.database.NotesDatabase;
import com.example.notesapplication.entities.Note;
import com.example.notesapplication.listeners.NotesListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NotesListener {

    public static final int REQUEST_CODE_ADD_NOTE =1;
    public static final int REQUEST_CODE_UPDATE_NOTE =2;
    public static final int REQUEST_CODE_SHOW_NOTE =3;

    private RecyclerView notesRecyclerView;
    private List<Note> notesList;
    private NotesAdapter notesAdapter;

    private int noteClickedPosition =-1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView imageAddNoteMain = findViewById(R.id.imageAddMain);
        imageAddNoteMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(getApplicationContext(), CreateNoteActivity.class),
                        REQUEST_CODE_ADD_NOTE);
            }
        });
        notesRecyclerView = findViewById(R.id.notesRecyclerView);
        notesRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        notesList = new ArrayList<>();
        notesAdapter = new NotesAdapter(notesList, this);
        notesRecyclerView.setAdapter(notesAdapter);

        getAllNotes(REQUEST_CODE_SHOW_NOTE, false);

        EditText inputSearch = findViewById(R.id.inputSearch);
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                notesAdapter.cancelTimer();
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(notesList.size() != 0){
                    notesAdapter.searchNotes(s.toString());
                }
            }
        });
    }
    private void getAllNotes(final int requestCode, final boolean isNoteDeleted){

        @SuppressLint("StaticFieldLeak")
        class  GetNotesTask extends AsyncTask<Void, Void, List<Note>>{

            @Override
            protected List<Note> doInBackground(Void... voids) {
                return NotesDatabase.getDatabase(getApplicationContext())
                        .noteDAO()
                        .getAllNotes();
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            protected void onPostExecute(List<Note> notes) {
                super.onPostExecute(notes);
               if(requestCode == REQUEST_CODE_SHOW_NOTE){
                   notesList.addAll(notes);
                   notesAdapter.notifyDataSetChanged();
               } else if(requestCode == REQUEST_CODE_ADD_NOTE){
                   notesList.add(0, notes.get(0));
                   notesAdapter.notifyItemInserted(0);
                   notesRecyclerView.smoothScrollToPosition(0);
               } else if(requestCode == REQUEST_CODE_UPDATE_NOTE){
                   notesList.remove(noteClickedPosition);
                   if(isNoteDeleted){
                       notesAdapter.notifyItemRemoved(noteClickedPosition);
                   } else {
                       notesList.add(noteClickedPosition, notes.get(noteClickedPosition));
                       notesAdapter.notifyItemChanged(noteClickedPosition);
                   }
               }
            }
        }
        new GetNotesTask().execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK && data!= null){
            getAllNotes(REQUEST_CODE_ADD_NOTE, false);
        } else if(requestCode == REQUEST_CODE_UPDATE_NOTE && resultCode == RESULT_OK){
            getAllNotes(REQUEST_CODE_UPDATE_NOTE, data.getBooleanExtra("isNoteDeleted", false));
        }
    }

    @Override
    public void onNoteClicked(Note note, int position) {
        noteClickedPosition = position;
        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
        intent.putExtra("isViewOrUpdate", true);
        intent.putExtra("note", note);
        startActivityForResult(intent, REQUEST_CODE_UPDATE_NOTE);
    }
}