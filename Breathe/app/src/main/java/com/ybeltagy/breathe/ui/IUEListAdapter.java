package com.ybeltagy.breathe.ui;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.ybeltagy.breathe.R;
import com.ybeltagy.breathe.data.DataFinals;
import com.ybeltagy.breathe.data.InhalerUsageEvent;

import java.util.List;

/**
 * This class prepares and updates the IUE data to be displayed in the RecyclerView in the Main
 * Activity.
 */
public class IUEListAdapter
        extends RecyclerView.Adapter<IUEListAdapter.IUEViewHolder> {

    // Cached copy of InhalerUsageEvents
    private List<InhalerUsageEvent> iueEntries;

    // Inflater
    private final LayoutInflater iueInflater;

    // Tells the recyclerview which item was clicked
    private static IUEListItemClickListener iueListItemClickListener;

    protected interface IUEListItemClickListener {
        void onItemClick(View v, int position);
    }

    protected IUEListAdapter(Context context) {
        iueInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public IUEViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = iueInflater.inflate(R.layout.iue_item, parent, false); // what happens if attachToRoot is true
        return new IUEViewHolder(itemView);
    }

    /**
     * I tested the database when it was empty. It simply didn't display anything. The case in which
     * iueEntries is null is not possible. Why is there an if statement?
     * @param holder
     * @param position
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBindViewHolder(@NonNull IUEViewHolder holder, int position) {

        // Get the the iue
        InhalerUsageEvent current = iueEntries.get(position);

        Log.d("IUEListAdapter", "setting text to "
                + current.getInhalerUsageEventTimeStamp().toString());

        // Display the iue.
        holder.setIUE(current);
    }

    public void setIUEs(List<InhalerUsageEvent> inhalerUsageEvents) {
        iueEntries = inhalerUsageEvents;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return iueEntries != null ? iueEntries.size() : 0;
    }

    /**
     * Identifies which InhalerUsageEvent was clicked for methods that handle user events
     *
     * @param position Position of InhalerUsageEvent in RecyclerView
     * @return The InhalerUsageEvent at the input position
     */
    protected InhalerUsageEvent getInhalerUsageEventAtPosition(int position) {
        return iueEntries.get(position);
    }

    protected void setOnItemClickListener(IUEListItemClickListener IUEListItemClickListener) {
        iueListItemClickListener = IUEListItemClickListener;
    }

    // Class that holds View information for displaying one item from the item's layout
    static class IUEViewHolder extends RecyclerView.ViewHolder {

        public final LinearLayout inhalerUsageEventEntry; // the entire entry in the timeline

        public final TextView iueTimeStamp;  // text view of the timestamp
        public final TextView diaryMessage; // text view of the diary entry
        public final TextView tag;          // text view of the tag

        public IUEViewHolder(@NonNull View itemView) {
            super(itemView);
            inhalerUsageEventEntry = itemView.findViewById(R.id.inhaler_usage_event_entry);
            iueTimeStamp = itemView.findViewById(R.id.iue_entry_timestamp_textview);
            diaryMessage = itemView.findViewById(R.id.iue_entry_diary_textview);
            tag = itemView.findViewById(R.id.iue_entry_tag_textview);

            inhalerUsageEventEntry.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    iueListItemClickListener.onItemClick(view, getAbsoluteAdapterPosition());
                }
            });
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        protected void setIUE(InhalerUsageEvent current){
            // show timestamp
            iueTimeStamp.setText(current.getInhalerUsageEventTimeStamp().toString());


            diaryMessage.setText(current.getDiaryEntry().getMessage());


            // show tag
            if (current.getDiaryEntry().getTag() != DataFinals.DEFAULT_TAG) { // only print user-chosen tags
                tag.setText(String.format("Tagged as : %s",
                        current.getDiaryEntry().getTag().toString()));
            }
        }
    }
}
