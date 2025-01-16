package cs477.gmu.project2_ywaikar;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class AddEditProductActivity extends AppCompatActivity {
    private EditText nameEditText, descriptionEditText, priceEditText, quantityEditText;
    private Button saveButton, deleteButton;
    private ProductFirestoreHelper firestoreHelper;
    private String productId; // `null` indicates a new product

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_product);

        firestoreHelper = new ProductFirestoreHelper();

        nameEditText = findViewById(R.id.product_name_edit_text);
        descriptionEditText = findViewById(R.id.product_description_edit_text);
        priceEditText = findViewById(R.id.product_price_edit_text);
        quantityEditText = findViewById(R.id.product_quantity_edit_text);
        saveButton = findViewById(R.id.save_button);
        deleteButton = findViewById(R.id.delete_button);

        if (getIntent().hasExtra("PRODUCT_ID")) {
            productId = getIntent().getStringExtra("PRODUCT_ID");
            loadProductDetails(productId);
        } else {
            deleteButton.setVisibility(View.GONE);
        }

        saveButton.setOnClickListener(view -> saveProduct());
        deleteButton.setOnClickListener(view -> deleteProduct());
    }

    private void loadProductDetails(String productId) {
        firestoreHelper.getProduct(productId, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Product product = task.getResult().toObject(Product.class);
                if (product != null) {
                    nameEditText.setText(product.getName());
                    descriptionEditText.setText(product.getDescription());
                    priceEditText.setText(String.valueOf(product.getPrice()));
                    quantityEditText.setText(String.valueOf(product.getQuantity()));
                }
            } else {
                Toast.makeText(this, "Failed to load product.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProduct() {
        String name = nameEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String priceStr = priceEditText.getText().toString().trim();
        String quantityStr = quantityEditText.getText().toString().trim();

        // Validate inputs
        if (name.isEmpty() || priceStr.isEmpty() || quantityStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = Double.parseDouble(priceStr);
        int quantity = Integer.parseInt(quantityStr);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (productId == null) { // New product
            Product product = new Product(null, name, description, price, quantity);
            db.collection("products")
                    .add(product)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Product added.", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error saving product: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else { // Update existing product
            db.collection("products").document(productId)
                    .update("name", name,
                            "description", description,
                            "price", price,
                            "quantity", quantity)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Product updated.", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error updating product: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void deleteProduct() {
        if (productId != null) {
            firestoreHelper.deleteProduct(productId, task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Product deleted.", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Error deleting product.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}