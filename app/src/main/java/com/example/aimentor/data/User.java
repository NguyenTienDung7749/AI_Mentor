package com.example.aimentor.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** A registered student account. Passwords are stored hashed + salted only. */
@Entity(tableName = "users", indices = {@Index(value = "email", unique = true)})
public class User {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String email = "";

    public String name = "";
    public String passwordHash = "";
    public String salt = "";

    // Onboarding preferences
    public String educationLevel = "";   // Middle School / High School / University
    public String subjects = "";         // comma separated list of subjects of interest
    public String explanationStyle = ""; // Short / Detailed / Step-by-step

    public int xp = 0;
    public boolean onboardingCompleted = false;
    public long createdAt = System.currentTimeMillis();
}
