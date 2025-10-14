package org.levimc.launcher.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.levimc.launcher.R;

import java.util.ArrayList;
import java.util.List;

public class QuickActionsAdapter extends RecyclerView.Adapter<QuickActionsAdapter.ViewHolder> {

    public interface OnActionClickListener {
        void onActionClick(int actionId);
    }

    public static class QuickActionItem {
        public final int titleRes;
        public final int subtitleRes;
        public final int actionId;

        public QuickActionItem(int titleRes, int subtitleRes, int actionId) {
            this.titleRes = titleRes;
            this.subtitleRes = subtitleRes;
            this.actionId = actionId;
        }
    }

    private final List<QuickActionItem> items = new ArrayList<>();
    private OnActionClickListener onActionClickListener;

    public QuickActionsAdapter(List<QuickActionItem> initialItems) {
        if (initialItems != null) items.addAll(initialItems);
    }

    public void setOnActionClickListener(OnActionClickListener listener) {
        this.onActionClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quick_action, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QuickActionItem item = items.get(position);
        holder.title.setText(item.titleRes);
        holder.subtitle.setText(item.subtitleRes);
        // 增加顶部外边距以匹配原第二项的间距
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
        if (lp != null) {
            lp.topMargin = (position == 0) ? 0 : dpToPx(holder.itemView, 6);
            holder.itemView.setLayoutParams(lp);
        }

        holder.itemView.setOnClickListener(v -> {
            if (onActionClickListener != null) {
                onActionClickListener.onActionClick(item.actionId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateItems(List<QuickActionItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView subtitle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.qa_title);
            subtitle = itemView.findViewById(R.id.qa_subtitle);
        }
    }

    private int dpToPx(View view, int dp) {
        float density = view.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }
}