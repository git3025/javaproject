package com.example.demo.repository;

import com.example.demo.entity.ViewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ViewRepository extends JpaRepository<ViewQuestion, Long> {

    List<ViewQuestion> findByISBNAndPages(String ISBN, Integer pages);
}