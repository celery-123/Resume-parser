package com.example.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ParsedResume {
    private PersonalInfo personalInfo = new PersonalInfo();
    private List<WorkExperience> workExperiences = new ArrayList<>();
    private List<Education> educations = new ArrayList<>();
    private List<String> skills = new ArrayList<>();
    private List<String> certifications = new ArrayList<>();
    private String rawText;
    private String fileName;

    @Data
    public static class PersonalInfo {
        private String name;
        private String email;
        private String phone;
        private String location;
        private Integer yearsOfExperience;
    }

    @Data
    public static class WorkExperience {
        private String company;
        private String position;
        private String duration;
        private String description;
        private List<String> technologies = new ArrayList<>();
    }

    @Data
    public static class Education {
        private String institution;
        private String degree;
        private String major;
        private String period;
    }
}
