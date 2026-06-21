package com.example.demo.Models;

import jakarta.persistence.*;

@Entity
@Table(name = "cafe_tables")
public class CafeTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tableNumber;

    @Column(unique = true)
    private String qrCode;

    private int capacity = 4;

    private Integer layoutX;
    private Integer layoutY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TableStatus status = TableStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merged_into_id")
    private CafeTable mergedInto;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTableNumber() { return tableNumber; }
    public void setTableNumber(String tableNumber) { this.tableNumber = tableNumber; }
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public Integer getLayoutX() { return layoutX; }
    public void setLayoutX(Integer layoutX) { this.layoutX = layoutX; }
    public Integer getLayoutY() { return layoutY; }
    public void setLayoutY(Integer layoutY) { this.layoutY = layoutY; }
    public TableStatus getStatus() { return status; }
    public void setStatus(TableStatus status) { this.status = status; }
    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }
    public CafeTable getMergedInto() { return mergedInto; }
    public void setMergedInto(CafeTable mergedInto) { this.mergedInto = mergedInto; }

    public boolean isMerged() {
        return mergedInto != null;
    }
}
