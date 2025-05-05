package org.levimc.launcher.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.levimc.launcher.R;
import org.levimc.launcher.core.versions.GameVersion;
import org.levimc.launcher.ui.dialogs.gameversionselect.BigGroup;
import org.levimc.launcher.ui.dialogs.gameversionselect.UltimateVersionAdapter;

import java.util.List;

public class GameVersionSelectDialog extends Dialog {
    public interface OnVersionSelectListener {
        void onVersionSelected(GameVersion version);
    }

    private OnVersionSelectListener listener;
    private List<BigGroup> bigGroups;

    public GameVersionSelectDialog(@NonNull Context ctx, List<BigGroup> bigGroups) {
        super(ctx);
        this.bigGroups = bigGroups;
    }

    public void setOnVersionSelectListener(OnVersionSelectListener l) {
        this.listener = l;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_game_version_select);
        RecyclerView recyclerView = findViewById(R.id.recycler_versions);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        UltimateVersionAdapter adapter = new UltimateVersionAdapter(getContext(), bigGroups);
        adapter.setOnVersionSelectListener(v -> {
            if (listener != null) listener.onVersionSelected(v);
            dismiss();
        });
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        recyclerView.setAdapter(adapter);
    }
}