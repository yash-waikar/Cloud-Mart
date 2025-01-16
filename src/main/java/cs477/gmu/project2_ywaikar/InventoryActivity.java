package cs477.gmu.project2_ywaikar;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class InventoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private List<Product> productList;
    private List<Product> filteredList;
    private FloatingActionButton addProductFab;
    private EditText searchBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        // initialize product list
        productList = new ArrayList<>();
        filteredList = new ArrayList<>();

        recyclerView = findViewById(R.id.inventory_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductAdapter(this, filteredList);
        recyclerView.setAdapter(adapter);

        addProductFab = findViewById(R.id.add_product_fab);
        addProductFab.setOnClickListener(view -> {

            Intent intent = new Intent(this, AddEditProductActivity.class);
            startActivity(intent);
        });

        searchBar = findViewById(R.id.search_bar);
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No-op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProducts(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No-op
            }
        });

        Button fetchStockButton = findViewById(R.id.fetch_stock_button);
        fetchStockButton.setOnClickListener(v -> fetchAndDisplayStock());
    }

    private void fetchAndDisplayStock() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("products")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    productList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Product product = document.toObject(Product.class);
                        product.setId(document.getId());
                        productList.add(product);
                    }
                    adapter.notifyDataSetChanged();
                    filterProducts(searchBar.getText().toString()); // filter  list
                    Toast.makeText(this, "Stock updated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch stock: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void listenForProductUpdates() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("products")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Error listening for updates: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots != null) {
                        productList.clear(); // Clear the existing list

                        for (DocumentSnapshot document : snapshots.getDocuments()) {
                            Product product = document.toObject(Product.class);
                            if (product != null) {
                                product.setId(document.getId()); // Set the document ID
                                productList.add(product);
                            }
                        }

                        filterProducts(searchBar.getText().toString()); // Filter updated list
                    }
                });
    }

    private void filterProducts(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(productList);
        } else {
            for (Product product : productList) {
                if (product.getName().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(product);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        listenForProductUpdates();
    }
}