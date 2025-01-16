package cs477.gmu.project2_ywaikar;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class OrderActivity extends AppCompatActivity {

    private Spinner productSpinner;
    private EditText quantityEditText;
    private Button addButton, removeButton, finishButton;
    private ListView orderListView;
    private TextView totalPriceTextView;

    private FirebaseFirestore db;
    private CollectionReference cartRef;
    private List<Product> productList;
    private ArrayAdapter<String> spinnerAdapter;
    private ArrayAdapter<String> orderListAdapter;
    private List<String> orderItemList;
    private List<OrderItem> orderItems;
    private double totalPrice = 0.0;

    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        // Get current user ID
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();

        // Firestore initialization
        db = FirebaseFirestore.getInstance();
        cartRef = db.collection("users").document(userId).collection("cart");

        productSpinner = findViewById(R.id.product_spinner);
        quantityEditText = findViewById(R.id.quantity_edit_text);
        addButton = findViewById(R.id.add_button);
        removeButton = findViewById(R.id.remove_button);
        finishButton = findViewById(R.id.finish_button);
        orderListView = findViewById(R.id.order_list_view);
        totalPriceTextView = findViewById(R.id.total_price_text_view);

        productList = new ArrayList<>();
        orderItemList = new ArrayList<>();
        orderItems = new ArrayList<>();
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        productSpinner.setAdapter(spinnerAdapter);

        orderListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, orderItemList);
        orderListView.setAdapter(orderListAdapter);

        fetchProducts();
        fetchCart();

        addButton.setOnClickListener(view -> addItemToCart());
        removeButton.setOnClickListener(view -> removeItemFromCart());
        finishButton.setOnClickListener(view -> finalizeOrder());
    }

    private void fetchProducts() {
        db.collection("products").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                productList.clear();
                spinnerAdapter.clear();

                for (QueryDocumentSnapshot document : task.getResult()) {
                    Product product = document.toObject(Product.class);
                    product.setId(document.getId()); // Set Firestore document ID
                    productList.add(product);
                    spinnerAdapter.add(product.getName());
                }

                spinnerAdapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "Failed to fetch products.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchCart() {
        cartRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                orderItems.clear();
                orderItemList.clear();

                for (QueryDocumentSnapshot document : task.getResult()) {
                    OrderItem item = document.toObject(OrderItem.class);
                    orderItems.add(item);
                    orderItemList.add(item.getProductName() + " - Quantity: " + item.getQuantity());
                    totalPrice += item.getQuantity() * item.getPricePerUnit();
                }

                orderListAdapter.notifyDataSetChanged();
                totalPriceTextView.setText("Total Price: $" + String.format("%.2f", totalPrice));
            } else {
                Toast.makeText(this, "Failed to fetch cart.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addItemToCart() {
        String productName = productSpinner.getSelectedItem().toString();
        String quantityStr = quantityEditText.getText().toString().trim();

        if (quantityStr.isEmpty()) {
            Toast.makeText(this, "Please enter a quantity.", Toast.LENGTH_SHORT).show();
            return;
        }

        int quantity = Integer.parseInt(quantityStr);
        Product product = getProductByName(productName);

        if (product != null) {
            int availableQuantity = product.getQuantity();
            OrderItem existingOrderItem = findOrderItemByProductId(product.getId());

            int totalRequestedQuantity = quantity;
            if (existingOrderItem != null) {
                totalRequestedQuantity += existingOrderItem.getQuantity();
            }

            if (totalRequestedQuantity > availableQuantity) {
                Toast.makeText(this, "Only " + availableQuantity + " units available.", Toast.LENGTH_SHORT).show();
                return;
            }

            OrderItem orderItem = new OrderItem(product.getId(), product.getName(), quantity, product.getPrice());
            cartRef.document(product.getId()).set(orderItem).addOnSuccessListener(aVoid -> {
                fetchCart();
                Toast.makeText(this, "Added to cart.", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to add to cart: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void removeItemFromCart() {
        String productName = productSpinner.getSelectedItem().toString();
        OrderItem orderItem = findOrderItemByProductName(productName);

        if (orderItem != null) {
            cartRef.document(orderItem.getProductId()).delete().addOnSuccessListener(aVoid -> {
                fetchCart();
                Toast.makeText(this, "Item removed from cart.", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to remove item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            Toast.makeText(this, "Item not in cart.", Toast.LENGTH_SHORT).show();
        }
    }

    private void finalizeOrder() {
        cartRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    OrderItem item = document.toObject(OrderItem.class);
                    db.collection("products").document(item.getProductId()).get().addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Product product = documentSnapshot.toObject(Product.class);
                            if (product != null) {
                                int newQuantity = product.getQuantity() - item.getQuantity();
                                if (newQuantity <= 0) {
                                    db.collection("products").document(item.getProductId()).delete();
                                } else {
                                    db.collection("products").document(item.getProductId()).update("quantity", newQuantity);
                                }
                            }
                        }
                    });
                }

                cartRef.get().addOnCompleteListener(innerTask -> {
                    if (innerTask.isSuccessful()) {
                        for (QueryDocumentSnapshot document : innerTask.getResult()) {
                            cartRef.document(document.getId()).delete();
                        }
                        fetchCart();
                        Toast.makeText(this, "Order completed.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private Product getProductByName(String productName) {
        for (Product product : productList) {
            if (product.getName().equals(productName)) {
                return product;
            }
        }
        return null;
    }

    private OrderItem findOrderItemByProductId(String productId) {
        for (OrderItem item : orderItems) {
            if (item.getProductId().equals(productId)) {
                return item;
            }
        }
        return null;
    }

    private OrderItem findOrderItemByProductName(String productName) {
        for (OrderItem item : orderItems) {
            if (item.getProductName().equals(productName)) {
                return item;
            }
        }
        return null;
    }
}