package uni.mobile.mobileapp;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uni.mobile.mobileapp.guiAdapters.ListAdapter;
import uni.mobile.mobileapp.rest.Book;
import uni.mobile.mobileapp.rest.DownloadImageTask;
import uni.mobile.mobileapp.rest.MyResponse;
import uni.mobile.mobileapp.rest.RestLocalMethods;
import uni.mobile.mobileapp.rest.Shelf;
import uni.mobile.mobileapp.rest.callbacks.BookCallback;


public class BookFragment extends Fragment {

    private SwipeRefreshLayout srl;
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton addBookButton;
    private MaterialCardView cardView;
    private ImageView googleImage;
    private TextView googleTitle;
    private TextView googleAuthors;
    private TextView googleDesc;
    private MaterialButton ignoreButton;
    private MaterialButton acceptButton;
    private ListView lView;
    private Book currentBook;
    private Spinner userShelves;
    private List<Shelf> shelves = null;
    private Map<String, Shelf> shelfDic = null;
    private String currentShelf = "Select a shelf!";
    private Context context;
    private FragmentActivity activity;
    private Handler mHandler = new Handler(Looper.getMainLooper());


    public BookFragment(BottomNavigationView bottomNavigationView) {
        this.bottomNavigationView = bottomNavigationView;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_book, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        context = this.getContext();
        activity = this.getActivity();

        //layout
        srl=view.findViewById(R.id.swipeLayout);


        //fab
        addBookButton = view.findViewById(R.id.addBookButton);
        addBookButton.setClickable(false);

        RestLocalMethods.initRetrofit(context);

        // Load user books
        getUserBooks();


        //car related
        cardView = view.findViewById(R.id.card);
        googleImage= view.findViewById(R.id.googleImage);
        googleTitle = view.findViewById(R.id.googleTitle );
        googleAuthors = view.findViewById(R.id.googleAuthors);
        googleDesc = view.findViewById(R.id.googleDesc);
        ignoreButton = view.findViewById(R.id.ignoreButton);
        acceptButton = view.findViewById(R.id.acceptButton);

        ignoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentBook.setGoogleData(null);

                patchAndUpdate(view);

                cardView.setVisibility(View.GONE);
                lView.setClickable(true);
                getUserBooks();

            }
        });
        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentBook.updateBookWithGoogleData();

                patchAndUpdate(view);

                cardView.setVisibility(View.GONE);
                lView.setClickable(true);
                getUserBooks();

            }
        });

        // Custom choices
        List<String> choices = new ArrayList<>();
        choices.add("Select a shelf!");

        Call<MyResponse<Shelf>> call = RestLocalMethods.getJsonPlaceHolderApi().getAllShelves(RestLocalMethods.getMyUserId());
        call.enqueue(new Callback<MyResponse<Shelf>>() {
            @Override
            public void onResponse(Call<MyResponse<Shelf>> call, Response<MyResponse<Shelf>> response) {
                if(!response.isSuccessful()) return;
                shelves = response.body().getData();

                shelfDic = new HashMap<String, Shelf>();
                for(Shelf shelf: shelves){
                    choices.add(shelf.getName());
                    shelfDic.put(shelf.getName(), shelf);
                }
                addBookButton.setClickable(true);

            }

            @Override
            public void onFailure(Call<MyResponse<Shelf>> call, Throwable t) {
                Log.d("SHELF","Request Error :: " + t.getMessage() );
            }

        });

        // Create an ArrayAdapter with custom choices
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.item_spinner, choices){
            @Override
            public boolean isEnabled(int position){
                return position != 0;
            }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if(position == 0) {
                    // Set the hint text color gray
                    tv.setTextColor(Color.GRAY);
                }
                return view;
            }
        };

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(R.layout.item_spinner);

        //fab related
        addBookButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // User Can't add a Book if he has not a Shelf
                if (shelves.isEmpty()) {
                    new MaterialAlertDialogBuilder(context)
                            .setTitle("No shelf found")
                            .setMessage("Please create one first.")
                            .setCancelable(false) // disallow cancel of AlertDialog on click of back button and outside touch
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    bottomNavigationView.setSelectedItemId(R.id.navigation_shelf);
                                    FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
                                    transaction.replace(R.id.fragmentContainer, new ShelfFragment(bottomNavigationView));
                                    transaction.commit();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
                else {
                    LayoutInflater inflater = getLayoutInflater();
                    View alertLayout = inflater.inflate(R.layout.layout_custom_dialog_add_book, null);
                    final EditText bookTitleEditText = alertLayout.findViewById(R.id.bookTitle);
                    final EditText bookAuthorEditText = alertLayout.findViewById(R.id.bookAuthor);
                    final EditText bookISBNEditText = alertLayout.findViewById(R.id.bookISBN);
                    userShelves = alertLayout.findViewById(R.id.userShelves);

                    // Set the adapter to th spinner
                    userShelves.setAdapter(adapter);

                    userShelves.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            currentShelf = userShelves.getSelectedItem().toString();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });
                    new MaterialAlertDialogBuilder(context)
                            .setTitle("Create new book")
                            .setMessage("Insert the book name")
                            .setView(alertLayout) // this is set the view from XML inside AlertDialog
                            .setCancelable(false) // disallow cancel of AlertDialog on click of back button and outside touch
                            .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    addBookButton.setClickable(false);

                                    String bookTitle = bookTitleEditText.getText().toString();
                                    String bookAuthor = bookAuthorEditText.getText().toString();
                                    String bookISBN = bookISBNEditText.getText().toString();
                                    if (currentShelf.equals("Select a shelf!"))
                                        Toast.makeText(getContext(), "Please select a shelf!", Toast.LENGTH_SHORT).show();
                                    else {
                                        Shelf selectedShelf = shelfDic.get(currentShelf);
                                        Log.d("shelf", currentShelf);
                                        RestLocalMethods.createBook(RestLocalMethods.getMyUserId(),
                                                selectedShelf.getHouseId(), selectedShelf.getRoomId(),
                                                selectedShelf.getWallId(), selectedShelf.getId(),
                                                new Book(bookTitle, bookAuthor, bookISBN, selectedShelf.getId(),
                                                        selectedShelf.getWallId(), selectedShelf.getRoomId(),
                                                        selectedShelf.getHouseId()), new BookCallback() {
                                            @Override
                                            public void onSuccess(@NonNull List<Book> booksRes) {
                                                getUserBooks();
                                            }
                                        });
                                    }
                                    addBookButton.setClickable(true);
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            }
        });

        //List related

        lView = view.findViewById(R.id.bookList);


        srl.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getUserBooks();
            }
        });
    }


    public void getUserBooks() {
        RestLocalMethods.setContext(getContext());

        Call<MyResponse<Book>> callAsync = RestLocalMethods.getJsonPlaceHolderApi().getAllBooks(RestLocalMethods.getMyUserId());
        callAsync.enqueue(new Callback<MyResponse<Book>>() {

            @Override
            public void onResponse(Call<MyResponse<Book>> call, Response<MyResponse<Book>> response) {
                if (response.isSuccessful()) {

                    List<Book> books = response.body().getData();

                    ArrayList<String> titles = new ArrayList<String>();
                    ArrayList<String> authors = new ArrayList<String>();
                    ArrayList<String> images = new ArrayList<>();

                    for(Book b: books){
                        titles.add( b.getTitle() ) ;
                        authors.add(b.getAuthors());
                        if(b.getGoogleData()!=null && b.getSmallThumbnailUrl()!=null){
                            images.add(b.getSmallThumbnailUrl());
                        }
                        else{
                            images.add(null);
                        }
                    }


                    ListAdapter lAdapter = new ListAdapter(getContext(), titles, authors, null,R.drawable.ic_book_red);

                    lView.setAdapter(lAdapter);
                    if(getContext() == null)  //too late now to print
                        return;
                    Toast.makeText(getContext(), "Found " + books.size() +" books", Toast.LENGTH_SHORT).show();
                    Log.d("BOOK",titles.toString());
                    lView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                            //googleImage
                            currentBook = books.get(i);

                            if(currentBook.getGoogleData() != null && currentBook.getGoogleData().getVolumeInfo() != null ){
                           //     Toast.makeText(getContext(), "Google " + titles.get(i), Toast.LENGTH_SHORT).show();
                                if(currentBook.getGoogleData().getVolumeInfo().getImageLinks().getSmallThumbnail() != null)
                                    currentBook.setSmallThumbnail(currentBook.getGoogleData().getVolumeInfo().getImageLinks().getSmallThumbnail());
                                if(currentBook.getGoogleData().getVolumeInfo().getImageLinks().getThumbnail()!=null)
                                    new DownloadImageTask(googleImage).execute(currentBook.getGoogleData().getVolumeInfo().getImageLinks().getThumbnail());
                                if(currentBook.getGoogleData().getVolumeInfo().getTitle() != null) googleTitle.setText(currentBook.getGoogleData().getVolumeInfo().getTitle());
                                if(currentBook.getGoogleData().getVolumeInfo().getAuthors() != null) googleAuthors.setText(currentBook.getGoogleData().getVolumeInfo().getAuthors().toString() );
                                if(currentBook.getGoogleData().getVolumeInfo().getDescription() != null) googleDesc.setText(currentBook.getGoogleData().getVolumeInfo().getDescription());

                                lView.setClickable(false);
                                cardView.setVisibility(View.VISIBLE);
                            }
                            else{
                         //       Toast.makeText(getContext(), "No Google info for " + titles.get(i), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                    lView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                        @Override
                        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                            currentBook = books.get(position);
                            LayoutInflater inflater = getLayoutInflater();
                            View alertLayout = inflater.inflate(R.layout.layout_custom_dialog_edit_book, null);
                            final EditText bookTitleEditText = alertLayout.findViewById(R.id.bookTitleEdit);
                            final EditText bookAuthorEditText = alertLayout.findViewById(R.id.bookAuthorEdit);
                            final EditText bookISBNEditText = alertLayout.findViewById(R.id.bookISBNEdit);
                            bookTitleEditText.setText(currentBook.getTitle());
                            bookAuthorEditText.setText(currentBook.getAuthors());
                            bookISBNEditText.setText(currentBook.getIsbn());
                            final Button deleteBookButton = alertLayout.findViewById(R.id.deleteBookButton);
                            deleteBookButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    RestLocalMethods.deleteBook(RestLocalMethods.getMyUserId(), currentBook.getHouseId(), currentBook.getRoomId(), currentBook.getWallId(),currentBook.getShelfId(), currentBook.getId(), new BookCallback() {
                                        @Override
                                        public void onSuccess(@NonNull List<Book> booksRes) {
                                            getUserBooks();
                                        }
                                    });
                                }
                            });
                            new MaterialAlertDialogBuilder(context)
                                    .setTitle("Edit book")
                                    .setMessage("Change book title, authors and isbn or delete it")
                                    .setView(alertLayout) // this is set the view from XML inside AlertDialog
                                    .setCancelable(false) // disallow cancel of AlertDialog on click of back button and outside touch
                                    .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            RestLocalMethods.patchBook(RestLocalMethods.getMyUserId(), currentBook.getHouseId(), currentBook.getRoomId(), currentBook.getWallId(), currentBook.getShelfId(), currentBook.getId(), new Book(bookTitleEditText.getText().toString(), bookAuthorEditText.getText().toString(), bookISBNEditText.getText().toString(), currentBook.getShelfId(), currentBook.getWallId(), currentBook.getRoomId(), currentBook.getHouseId()), new BookCallback() {
                                                @Override
                                                public void onSuccess(@NonNull List<Book> booksRes) {
                                                    getUserBooks();
                                                }
                                            });
                                        }
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();

                            return true;

                        }
                    });


                }
                else {
                    Log.d("BOOK","Request Error :: " + response.errorBody());
                    Toast.makeText(getContext(), "Google api error", Toast.LENGTH_SHORT).show();
                }
                mHandler.post(new Runnable() { @Override public void run() { srl.setRefreshing(false); } });
            }

            @Override
            public void onFailure(Call<MyResponse<Book>> call, Throwable t) {
                Log.d("BOOK","Request Error :: " + t.getMessage() );
            }
        });
    }


    private static Drawable LoadImageFromWebOperations(String url) {
        try {
            InputStream is = (InputStream) new URL(url).getContent();
            Drawable d = Drawable.createFromStream(is, "src name");
            return d;
        } catch (Exception e) {
            return null;
        }
    }


    private void patchAndUpdate(View view){
        RestLocalMethods.patchBook(RestLocalMethods.getMyUserId(), currentBook.getHouseId(), currentBook.getRoomId(), currentBook.getWallId(), currentBook.getShelfId(), currentBook.getId(), currentBook, new BookCallback() {
            @Override
            public void onSuccess(@NonNull List<Book> booksRes) {
                getUserBooks();
            }
        });
    }


}
