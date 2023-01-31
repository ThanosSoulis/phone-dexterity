package com.example.phonedexterityTraining;

import android.util.Log;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class record implements Serializable {

    private String uId;
    private String date;
    private int time; // in seconds
    private int spinRep;
    private float spinSpeed;
    private float spinScore;
    private int rotateRep;
    private float rotateSpeed;
    private float rotateScore;
    private int flipRep;
    private float flipSpeed;
    private float flipScore;

    public record() {
        // Default constructor required for calls to DataSnapshot.getValue(Post.class)
    }

    public record(String uid, String date, int time, int spinRep, float spinSpeed, float spinScore,
                  int rotateRep, float rotateSpeed, float rotateScore, int flipRep, float flipSpeed, float flipScore) {
        this.uId = uid;
        this.date = date;
        this.time = time;
        this.spinRep = spinRep;
        this.spinSpeed = spinSpeed;
        this.spinScore = spinScore;
        this.rotateRep = rotateRep;
        this.rotateSpeed = rotateSpeed;
        this.rotateScore = rotateScore;
        this.flipRep = flipRep;
        this.flipSpeed = flipSpeed;
        this.flipScore = flipScore;
    }

    public void updateRecord(int time, int spinRep, float spinSpeed, float spinScore,
                             int rotateRep, float rotateSpeed, float rotateScore, int flipRep, float flipSpeed, float flipScore) {
        this.time += time;
        this.spinRep += spinRep;
        this.spinSpeed += spinSpeed;
        this.spinScore += spinScore;
        this.rotateRep += rotateRep;
        this.rotateSpeed += rotateSpeed;
        this.rotateScore += rotateScore;
        this.flipRep += flipRep;
        this.flipSpeed += flipSpeed;
        this.flipScore += flipScore;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("uId", uId);
        result.put("date", date);
        result.put("time", time);
        result.put("spinRep", spinRep);
        result.put("spinSpeed", spinSpeed);
        result.put("spinScore", spinScore);
        result.put("rotateRep", rotateRep);
        result.put("rotateSpeed", rotateSpeed);
        result.put("rotateScore", rotateScore);
        result.put("flipRep", flipRep);
        result.put("flipSpeed", flipSpeed);
        result.put("flipScore", flipScore);

        return result;
    }

    public LocalDate getDate(){
        //System.out.println(this.date);
        return LocalDate.parse(this.date);
    }

    public float getAverageScore(){
        if(this.spinRep + this.rotateRep + this.flipRep == 0){
            return 0;
        }
        return (float)(this.spinScore + this.rotateScore + this.flipScore) / (float)(this.spinRep + this.rotateRep + this.flipRep);
    }

    public float getAverageSpeed(){
        if(this.spinRep + this.rotateRep + this.flipRep == 0){
            return 0;
        }
        return (float)(this.spinSpeed + this.rotateSpeed + this.flipSpeed) / (float)(this.spinRep + this.rotateRep + this.flipRep);
    }

    public int getTime(){
        return this.time;
    }

    public int getSpinRep(){
        return this.spinRep;
    }

    public float getSpinSpeed(){
        if(this.spinRep == 0){
            return 0f;
        }
        return this.spinSpeed / (float)this.spinRep;
    }

    public float getSpinScore(){
        if(this.spinRep == 0){
            return 0f;
        }
        return this.spinScore / (float)this.spinRep;
    }

    public int getRotateRep(){
        return this.rotateRep;
    }

    public float getRotateSpeed(){
        if(this.rotateRep == 0){
            return 0f;
        }
        return this.rotateSpeed / (float)this.rotateRep;
    }

    public float getRotateScore(){
        if(this.rotateRep == 0){
            return 0f;
        }
        return this.rotateScore / (float)this.rotateRep;
    }

    public int getFlipRep(){
        return this.flipRep;
    }

    public float getFlipSpeed(){
        if(this.flipRep == 0){
            return 0f;
        }
        return this.flipSpeed / (float)this.flipRep;
    }

    public float getFlipScore(){
        if(this.flipRep == 0){
            return 0f;
        }
        return this.flipScore / (float)this.flipRep;
    }
}