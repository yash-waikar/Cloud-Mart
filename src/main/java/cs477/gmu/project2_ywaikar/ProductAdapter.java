package cs477.gmu.project2_ywaikar;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {
    private final List<Product> productList;
    private final Context context;

    public ProductAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView nameTextView, priceTextView, quantityTextView;

        public ViewHolder(View itemView) {
            super(itemView);

            nameTextView = itemView.findViewById(R.id.product_name_text_view);
            priceTextView = itemView.findViewById(R.id.product_price_text_view);
            quantityTextView = itemView.findViewById(R.id.product_quantity_text_view);

            // Long click to delete product
            itemView.setOnLongClickListener(view -> {
                int position = getAdapterPosition();
                Product product = productList.get(position);

                confirmDelete(product);
                return true;
            });
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.inventory_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ProductAdapter.ViewHolder holder, int position) {
        Product product = productList.get(position);

        Toast.makeText(context, "Product: " + product.getName() + ", Quantity: " + product.getQuantity(), Toast.LENGTH_SHORT).show();
        holder.nameTextView.setText(product.getName());
        holder.priceTextView.setText("Price: $" + String.format("%.2f", product.getPrice()));

        if (product.getQuantity() < 5) {
            holder.quantityTextView.setTextColor(Color.RED);
            holder.quantityTextView.setText("Low stock: " + product.getQuantity());
            Toast.makeText(context, product.getName() + " stock is low! Only " + product.getQuantity() + " left.", Toast.LENGTH_SHORT).show();
        } else {
            holder.quantityTextView.setTextColor(Color.BLACK);
            holder.quantityTextView.setText("Quantity: " + product.getQuantity() + " left");
        }
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    private void confirmDelete(Product product) {
        new android.app.AlertDialog.Builder(context)
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete " + product.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> deleteProduct(product))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteProduct(Product product) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String productId = product.getId();

        if (productId != null) {
            db.collection("products").document(productId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        productList.remove(product);
                        notifyDataSetChanged();
                        Toast.makeText(context, "Product deleted.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(context, "Failed to delete product: " + e.getMessage(), Toast.LENGTH_LONG).show());
        } else {
            Toast.makeText(context, "Unable to delete product!", Toast.LENGTH_LONG).show();
        }
    }
}