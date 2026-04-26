package com.psd.repo;

import com.psd.entity.General;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GeneralRepository extends JpaRepository<General, Long> {
}