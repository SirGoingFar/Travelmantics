package com.eemf.sirgoingfar.travelmantics.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.eemf.sirgoingfar.travelmantics.R;
import com.eemf.sirgoingfar.travelmantics.models.Item;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CatalogActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 0;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private FloatingActionButton fbAddItem;
    private RecyclerView rvItem;
    private FrameLayout mEmptyStateContainer;
    private Adapter adapter;

    private DatabaseReference mItemDbReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (mAuth.getCurrentUser() == null)
                    SignIn();

            }
        };

        fbAddItem = findViewById(R.id.fab_add_new_item);
        mEmptyStateContainer = findViewById(R.id.fl_empty_state_view);

        adapter = new Adapter();
        rvItem = findViewById(R.id.rv_item_catalog);
        rvItem.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rvItem.setHasFixedSize(true);
        rvItem.setAdapter(adapter);

        fbAddItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(CatalogActivity.this, AddItemActivity.class));
            }
        });

        mEmptyStateContainer.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAuth.addAuthStateListener(mAuthListener);

        mItemDbReference = FirebaseDatabase.getInstance().getReference(FirebaseAuth.getInstance().getUid());
        mItemDbReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                swapData(prepareTodoList(dataSnapshot.getChildren()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void swapData(List<Item> itemList) {
        if (itemList.isEmpty())
            mEmptyStateContainer.setVisibility(View.VISIBLE);
        else {
            adapter.swapData(itemList);
            mEmptyStateContainer.setVisibility(View.GONE);
        }
    }

    private List<Item> prepareTodoList(Iterable<DataSnapshot> itemNodeChildren) {
        List<Item> itemList = new ArrayList<>();
        for (DataSnapshot child : itemNodeChildren) {
            itemList.add(child.getValue(Item.class));
        }
        return itemList;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAuth.removeAuthStateListener(mAuthListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_signout) {
            SignOut();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void SignIn() {
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN);
    }

    private void SignOut() {
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    public void onComplete(@NonNull Task<Void> task) {
                        SignIn();
                    }
                });
        mAuth.removeAuthStateListener(mAuthListener);
    }

    class Adapter extends RecyclerView.Adapter<Adapter.Holder> {

        private List<Item> mData = new ArrayList<>();

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new Holder(LayoutInflater.from(CatalogActivity.this).inflate(R.layout.layout_item, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int i) {
            Item currentItem = holder.getCurrentItem();

            holder.label.setText(currentItem.getLabel());
            holder.desc.setText(currentItem.getDesc());
            holder.value.setText(currentItem.getValue());
            Picasso.get()
                    .load(currentItem.getUrl())
                    .into(holder.icon);
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        public void swapData(List<Item> itemList) {
            if (itemList == null)
                return;

            mData = itemList;
            notifyDataSetChanged();
        }

        class Holder extends RecyclerView.ViewHolder {

            private TextView label;
            private TextView desc;
            private TextView value;
            private ImageView icon;

            Holder(@NonNull View itemView) {
                super(itemView);
                label = itemView.findViewById(R.id.tv_label);
                desc = itemView.findViewById(R.id.tv_desc);
                value = itemView.findViewById(R.id.tv_value);
                icon = itemView.findViewById(R.id.iv_picture);
            }

            Item getCurrentItem() {
                return mData.get(getAdapterPosition());
            }
        }
    }
}
