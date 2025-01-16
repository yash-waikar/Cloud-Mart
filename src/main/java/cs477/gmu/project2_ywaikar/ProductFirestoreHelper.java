package cs477.gmu.project2_ywaikar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.*;
import java.util.*;

public class ProductFirestoreHelper {
    private final FirebaseFirestore db;
    private final CollectionReference productsCollection;

    public ProductFirestoreHelper() {
        db = FirebaseFirestore.getInstance();
        productsCollection = db.collection("products");
    }

    // Add a product
    public void addProduct(Product product, OnCompleteListener<Void> onCompleteListener) {
        productsCollection.document(product.getName())
                .set(product)
                .addOnCompleteListener(onCompleteListener);
    }

    // Update a product
    public void updateProduct(String productId, Map<String, Object> updates, OnCompleteListener<Void> onCompleteListener) {
        productsCollection.document(productId)
                .update(updates)
                .addOnCompleteListener(onCompleteListener);
    }

    // Delete a product
    public void deleteProduct(String productId, OnCompleteListener<Void> onCompleteListener) {
        productsCollection.document(productId)
                .delete()
                .addOnCompleteListener(onCompleteListener);
    }

    // Get a single product
    public void getProduct(String productId, OnCompleteListener<DocumentSnapshot> onCompleteListener) {
        productsCollection.document(productId)
                .get()
                .addOnCompleteListener(onCompleteListener);
    }

    // Get all products
    public void getAllProducts(OnCompleteListener<QuerySnapshot> onCompleteListener) {
        productsCollection.get().addOnCompleteListener(onCompleteListener);
    }
}