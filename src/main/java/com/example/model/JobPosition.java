package com.example.model;

import lombok.Data;
import java.util.List;

@Data
public class JobPosition {
    private String id;
    private String title;
    private String company;
    private List<String> requiredSkills;
    private Integer minExperience;
    private String requiredEducation;
    private String description;
    private String industry;
    private Double baseSalary;
}
