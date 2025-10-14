package org.levimc.launcher.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.FileHandler;
import org.levimc.launcher.core.mods.Mod;
import org.levimc.launcher.core.versions.VersionManager;
import org.levimc.launcher.ui.adapter.ModsAdapter;
import org.levimc.launcher.ui.views.MainViewModel;
import org.levimc.launcher.ui.views.MainViewModelFactory;
import java.util.ArrayList;
import java.util.List;

public class ModsFullscreenActivity extends AppCompatActivity {

    private RecyclerView modsRecycler;
    private ModsAdapter modsAdapter;
    private MainViewModel viewModel;
    private TextView totalModsCount;
    private TextView enabledModsCount;
    private ActivityResultLauncher<Intent> pickModLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mods_fullscreen);

        setupViews();
        setupViewModel();
        setupRecyclerView();
        pickModLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        FileHandler fileHandler = new FileHandler(this, viewModel, VersionManager.get(this));
                        fileHandler.processIncomingFilesWithConfirmation(result.getData(), new FileHandler.FileOperationCallback() {
                            @Override
                            public void onSuccess(int processedFiles) {
                                Toast.makeText(ModsFullscreenActivity.this, getString(R.string.files_processed, processedFiles), Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String errorMessage) {
                            }

                            @Override
                            public void onProgressUpdate(int progress) {
                            }
                        }, true);
                    }
                }
        );
    }

    private void setupViews() {
        ImageButton closeButton = findViewById(R.id.close_fullscreen_button);
        closeButton.setOnClickListener(v -> finish());

        Button addModButton = findViewById(R.id.add_mod_fullscreen_button);
        addModButton.setOnClickListener(v -> {
            startFilePicker();
        });

        totalModsCount = findViewById(R.id.total_mods_count);
        enabledModsCount = findViewById(R.id.enabled_mods_count);
    }
    
    private void startFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        pickModLauncher.launch(intent);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this, new MainViewModelFactory(getApplication())).get(MainViewModel.class);

        viewModel.getModsLiveData().observe(this, this::updateModsUI);
    }

    private void setupRecyclerView() {
        modsRecycler = findViewById(R.id.mods_recycler_fullscreen);
        modsAdapter = new ModsAdapter(new ArrayList<>());
        modsRecycler.setLayoutManager(new LinearLayoutManager(this));
        modsRecycler.setAdapter(modsAdapter);

        modsAdapter.setOnModClickListener((mod, position) -> {
            Intent intent = new Intent(this, ModDetailActivity.class);
            intent.putExtra("mod_filename", mod.getFileName());
            intent.putExtra("mod_position", position);
            startActivity(intent);
        });

        modsAdapter.setOnModEnableChangeListener((mod, enabled) -> {
            if (viewModel != null) {
                viewModel.setModEnabled(mod.getFileName(), enabled);
                updateModsCount(); 
            }
        });
        
        modsAdapter.setOnModReorderListener(reorderedMods -> {
            if (viewModel != null) {
                viewModel.reorderMods(reorderedMods);
                Toast.makeText(this, R.string.mod_reordered, Toast.LENGTH_SHORT).show();
            }
        });
        
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                modsAdapter.moveItem(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                modsAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
            }
        };
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(modsRecycler);
    }

    private void updateModsUI(List<Mod> mods) {
        if (modsAdapter != null) {
            modsAdapter.updateMods(mods);
            updateModsCount();
        }
    }

    private void updateModsCount() {
        List<Mod> mods = viewModel.getModsLiveData().getValue();
        if (mods != null) {
            int total = mods.size();
            int enabled = 0;
            for (Mod mod : mods) {
                if (mod.isEnabled()) {
                    enabled++;
                }
            }

            totalModsCount.setText(String.valueOf(total));
            enabledModsCount.setText(String.valueOf(enabled));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refreshMods();
        }
    }
    
    
}
