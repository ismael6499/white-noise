package com.agustin.whitenoise.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.agustin.whitenoise.R;
import com.agustin.whitenoise.model.SoundItem;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.List;

public class SoundAdapter extends RecyclerView.Adapter<SoundAdapter.SoundViewHolder> {

    public interface OnSoundActionListener {
        void onToggle(SoundItem item, boolean enabled, int volume);
        void onVolumeChanged(SoundItem item, int volume);
    }

    private final Context context;
    private final List<SoundItem> sounds;
    private final OnSoundActionListener listener;

    public SoundAdapter(Context context, List<SoundItem> sounds, OnSoundActionListener listener) {
        this.context = context;
        this.sounds = sounds;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SoundViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_sound_card, parent, false);
        return new SoundViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SoundViewHolder holder, int position) {
        SoundItem item = sounds.get(position);

        holder.imgIcon.setImageResource(item.getIconResId());
        holder.tvName.setText(item.getNameResId());
        holder.tvVolumePercent.setText(context.getString(R.string.volume) + ": " + item.getVolume() + "%");

        // Temporarily detach listener to avoid trigger during bind
        holder.switchSound.setOnCheckedChangeListener(null);
        holder.switchSound.setChecked(item.isPlaying());

        if (item.isPlaying()) {
            holder.card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.bg_card_active));
            holder.card.setStrokeColor(ContextCompat.getColor(context, R.color.accent_blue));
            holder.card.setStrokeWidth(3);
            holder.volumeContainer.setVisibility(View.VISIBLE);
            holder.seekVolume.setProgress(item.getVolume());
            holder.imgIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.accent_blue)));
        } else {
            holder.card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.bg_card));
            holder.card.setStrokeColor(ContextCompat.getColor(context, R.color.slider_track));
            holder.card.setStrokeWidth(1);
            holder.volumeContainer.setVisibility(View.GONE);
            holder.imgIcon.setImageTintList(null);
        }

        holder.card.setOnClickListener(v -> {
            boolean nextState = !item.isPlaying();
            item.setPlaying(nextState);
            notifyItemChanged(holder.getAdapterPosition());
            if (listener != null) {
                listener.onToggle(item, nextState, item.getVolume());
            }
        });

        holder.switchSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (item.isPlaying() != isChecked) {
                item.setPlaying(isChecked);
                notifyItemChanged(holder.getAdapterPosition());
                if (listener != null) {
                    listener.onToggle(item, isChecked, item.getVolume());
                }
            }
        });

        holder.seekVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    item.setVolume(progress);
                    holder.tvVolumePercent.setText(context.getString(R.string.volume) + ": " + progress + "%");
                    if (listener != null) {
                        listener.onVolumeChanged(item, progress);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    @Override
    public int getItemCount() {
        return sounds.size();
    }

    public List<SoundItem> getSounds() {
        return sounds;
    }

    static class SoundViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final ImageView imgIcon;
        final TextView tvName;
        final TextView tvVolumePercent;
        final SwitchMaterial switchSound;
        final LinearLayout volumeContainer;
        final SeekBar seekVolume;

        public SoundViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.cardSound);
            imgIcon = itemView.findViewById(R.id.imgSoundIcon);
            tvName = itemView.findViewById(R.id.tvSoundName);
            tvVolumePercent = itemView.findViewById(R.id.tvSoundVolumePercent);
            switchSound = itemView.findViewById(R.id.switchSound);
            volumeContainer = itemView.findViewById(R.id.volumeContainer);
            seekVolume = itemView.findViewById(R.id.seekVolume);
        }
    }
}
