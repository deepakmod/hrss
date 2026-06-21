INSERT INTO roles (role_name, description) VALUES 
('ROLE_ADMIN', 'Administrator with full system access'),
('ROLE_MANAGER', 'Manager who can manage team members, tasks, and leaves'),
('ROLE_EMPLOYEE', 'Employee who can view/update profile, apply for leaves, and view tasks');

-- Password is 'password' (bcrypt hash)
INSERT INTO users (employee_code, first_name, last_name, email, phone, password, status, created_by, updated_by) VALUES
('EMP001', 'System', 'Admin', 'admin@company.com', '+1234567890', '$2a$10$Y50UaMFOxteibQEYDFpxxOP5jG6y5S.u.eYwzW74D2sWfVp0a1t32', 'ACTIVE', 'SYSTEM', 'SYSTEM');

INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1);
