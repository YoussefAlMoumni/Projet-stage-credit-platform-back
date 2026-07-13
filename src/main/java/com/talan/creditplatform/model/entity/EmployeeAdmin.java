package com.talan.creditplatform.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "employee_admin")
public class EmployeeAdmin {

    @Id
    @Column(name = "employee_id")
    private Long employeeId;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "employee_id")
    private User employee;

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public User getEmployee() { return employee; }
    public void setEmployee(User employee) { this.employee = employee; }
}
