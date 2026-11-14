package com.aria.repository;
import com.aria.entity.StaffMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface StaffMemberRepository extends JpaRepository<StaffMember,Long> {
 Optional<StaffMember> findByUserId(Long id);
}
