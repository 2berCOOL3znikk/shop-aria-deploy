package com.aria.entity;

import jakarta.persistence.*;

/** A category override, or category "*" for the store-wide fallback. */
@Entity
@Table(name="pricing_policies", uniqueConstraints=@UniqueConstraint(name="uk_pricing_policy_category",columnNames="category"))
public class PricingPolicy {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=120) private String category="*";
    @Column(nullable=false) private int warningDays=30;
    @Column(nullable=false) private int criticalDays=7;
    @Column(nullable=false) private int clearanceDays=1;
    @Column(nullable=false) private int warningPercent=15;
    @Column(nullable=false) private int criticalPercent=30;
    @Column(nullable=false) private int clearancePercent=50;
    @Column(nullable=false) private int slowStockDays=180;
    @Column(nullable=false) private int slowStockPercent=10;
    @Column(nullable=false) private int minimumMarginPercent=0;
    public Long getId(){return id;}
    public String getCategory(){return category;}
    public void setCategory(String value){category=value;}
    public int getWarningDays(){return warningDays;}
    public void setWarningDays(int value){warningDays=value;}
    public int getCriticalDays(){return criticalDays;}
    public void setCriticalDays(int value){criticalDays=value;}
    public int getClearanceDays(){return clearanceDays;}
    public void setClearanceDays(int value){clearanceDays=value;}
    public int getWarningPercent(){return warningPercent;}
    public void setWarningPercent(int value){warningPercent=value;}
    public int getCriticalPercent(){return criticalPercent;}
    public void setCriticalPercent(int value){criticalPercent=value;}
    public int getClearancePercent(){return clearancePercent;}
    public void setClearancePercent(int value){clearancePercent=value;}
    public int getSlowStockDays(){return slowStockDays;}
    public void setSlowStockDays(int value){slowStockDays=value;}
    public int getSlowStockPercent(){return slowStockPercent;}
    public void setSlowStockPercent(int value){slowStockPercent=value;}
    public int getMinimumMarginPercent(){return minimumMarginPercent;}
    public void setMinimumMarginPercent(int value){minimumMarginPercent=value;}
}
