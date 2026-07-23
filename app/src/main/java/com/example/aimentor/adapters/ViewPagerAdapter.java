package com.example.aimentor.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.aimentor.Fragments.CategoryFragment;
import com.example.aimentor.Fragments.HomeFragment;
import com.example.aimentor.Fragments.QuizFragment;
import com.example.aimentor.Fragments.SettingsFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {
    private final int ItemCount = 4;
    public ViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new HomeFragment();
        }
        if (position == 1){
            return new CategoryFragment();
        }
        if (position == 2){
            return new QuizFragment();
        }
        if (position == 3) {
            return new SettingsFragment();
        }
        return new HomeFragment();
    }

    @Override
    public int getItemCount() {
        return ItemCount;
    }
}
