package com.example.keepfit.db.dao;

import com.example.keepfit.db.entity.Goal;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface GoalDao {
//    @Query("SELECT * FROM goal")
//    List<Goal> loadAllGoals();

    @Query("SELECT * FROM goal WHERE visible = 1")
    List<Goal> loadAllVisibleGoals();

    @Query("SELECT * FROM goal WHERE goalId = :id")
    Goal findGoalWithId(int id);

    @Query("SELECT * FROM goal WHERE visible = 1 AND name = :name")
    Goal findVisibleGoalWithName(String name);

    @Insert
    void insert(Goal goal);

    @Update
    void update(Goal goal);

    @Delete
    void delete(Goal goal);

    @Query("DELETE FROM goal")
    void nuke();

    @Query("DELETE FROM goal WHERE visible = 0")
    void deleteInvisibleGoals();
}
