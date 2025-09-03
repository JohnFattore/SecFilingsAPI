package com.example.sec_api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "assets")
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long cik;

/*
    @Column(name = "type", length = 25)
    private String type;
*/

    // --- getters and setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCik() { return cik; }
    public void setCik(Long cik) { this.cik = cik; }
/*
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
*/
}
