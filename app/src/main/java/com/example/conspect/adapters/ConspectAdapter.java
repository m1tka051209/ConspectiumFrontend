package com.example.conspect.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.conspect.databinding.ItemConspectBinding;
import com.example.conspect.models.Conspect;

import java.util.List;

public class ConspectAdapter extends RecyclerView.Adapter<ConspectAdapter.ConspectViewHolder> {
    private List<Conspect> conspects;
    private OnItemClickListener listener;
    private boolean showOfflineBadge = false;

    public interface OnItemClickListener {
        void onItemClick(Conspect conspect);
    }

    public ConspectAdapter(List<Conspect> conspects) {
        this.conspects = conspects;
    }

    public void setConspects(List<Conspect> conspects) {
        this.conspects = conspects;
        notifyDataSetChanged();
    }

    public void setShowOfflineBadge(boolean show) {
        this.showOfflineBadge = show;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ConspectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemConspectBinding binding = ItemConspectBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ConspectViewHolder(binding);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ConspectViewHolder holder, int position) {
        Conspect conspect = conspects.get(position);

        holder.binding.ttlCard.setText(conspect.getTitle());
        holder.binding.subj.setText("Предмет: " + conspect.getSubject());

        if (showOfflineBadge && !conspect.isSynced()) {
            holder.binding.offlineBadge.setVisibility(View.VISIBLE);
        } else {
            holder.binding.offlineBadge.setVisibility(View.GONE);
        }

        String content = conspect.getContent();
        String previewText = "";
        if (content != null && !content.isEmpty()) {
            previewText = (content.length() > 100) ? content.substring(0, 100) + "..." : content;
        }
        holder.binding.previewConsp.setText(previewText);

        holder.binding.date.setText(conspect.getFormattedDate());

        String updatedAt = conspect.getUpdatedAt();
        String createdAt = conspect.getCreatedAt();

        holder.binding.updatedDate.setVisibility(View.VISIBLE);

        if (updatedAt != null && !updatedAt.isEmpty()
                && createdAt != null
                && updatedAt.compareTo(createdAt) > 0) {
            holder.binding.updatedDate.setText(conspect.getFormattedUpdatedDate());
        } else {
            holder.binding.updatedDate.setText("Изменено: —");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(conspect);
            }
        });
    }

    @Override
    public int getItemCount() {
        return conspects == null ? 0 : conspects.size();
    }

    static class ConspectViewHolder extends RecyclerView.ViewHolder {
        private final ItemConspectBinding binding;

        public ConspectViewHolder(ItemConspectBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}