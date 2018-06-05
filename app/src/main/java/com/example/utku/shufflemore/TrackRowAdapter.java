package com.example.utku.shufflemore;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.utku.shufflemore.RandomSongProvider.Song;

import java.util.List;

public class TrackRowAdapter extends RecyclerView.Adapter<TrackRowAdapter.ViewHolder> {

    private List<Song> chosenSongs;
    private Playlist spotifyPlaylist;

    private int mExpandedPosition = -1;

    TrackRowAdapter(List<Song> chosenSongs, Playlist spotifyPlaylist) {

        this.chosenSongs = chosenSongs;
        this.spotifyPlaylist = spotifyPlaylist;
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        ImageView coverArt;
        TextView trackInfo;
        LinearLayout rowButtons;

        ViewHolder(View itemView) {

            super(itemView);

            coverArt = itemView.findViewById(R.id.cover_art);
            trackInfo = itemView.findViewById(R.id.track_info);
            rowButtons = itemView.findViewById(R.id.row_buttons);

            itemView.setOnClickListener(this);

            itemView.findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() {

                @SuppressLint("StaticFieldLeak")
                @Override
                public void onClick(View v) {

                    final int index = getAdapterPosition();
                    final Song clickedSong = chosenSongs.get(index);
                    System.out.println("clicked " + clickedSong.name);

                    new AsyncTask<Void , Void, Boolean>()
                    {
                        @Override
                        protected Boolean doInBackground (Void... v)  {
                            return spotifyPlaylist.removeTrack(clickedSong.uri);
                        }

                        @Override
                        protected void onPostExecute(Boolean success){

                            if (success) {

                                chosenSongs.remove(index);
                                notifyDataSetChanged();
                            }
                        }

                    }.execute();
                }
            });
        }

        @Override
        public void onClick(View v) {

            final int position = getAdapterPosition();
            final boolean isExpanded = position == mExpandedPosition;

            rowButtons.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            mExpandedPosition = isExpanded ? -1: position;
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View contactView = inflater.inflate(R.layout.recycler_row, parent, false);

        return new ViewHolder(contactView);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackRowAdapter.ViewHolder holder, final int position) {

        Song song = chosenSongs.get(position);

        TextView textView = holder.trackInfo;
        textView.setText(String.format("%s\n%s", song.name, song.artist));

        ImageView imageView = holder.coverArt;
        imageView.setImageBitmap(song.cover);
    }

    @Override
    public int getItemCount() {
            return chosenSongs.size();
    }
}
