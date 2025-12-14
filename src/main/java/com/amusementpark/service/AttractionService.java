package com.amusementpark.service;

import com.amusementpark.model.Attraction;
import com.amusementpark.repository.AttractionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AttractionService {
    
    private final AttractionRepository attractionRepository;
    
    @Autowired
    public AttractionService(AttractionRepository attractionRepository) {
        this.attractionRepository = attractionRepository;
    }
    
    public List<Attraction> getAllAttractions() {
        List<Attraction> attractions = attractionRepository.findAllWithOwner();
        System.out.println("AttractionService.getAllAttractions: найдено " + attractions.size() + " аттракционов");
        return attractions;
    }
    
    public List<Attraction> getAllAttractionsSorted(String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();
        return attractionRepository.findAll(sort);
    }
    
    public Optional<Attraction> getAttractionById(Long id) {
        return attractionRepository.findByIdWithOwner(id);
    }
    
    public Attraction saveAttraction(Attraction attraction) {
        System.out.println("AttractionService.saveAttraction: сохранение аттракциона " + (attraction.getId() != null ? "ID=" + attraction.getId() : "новый"));
        System.out.println("  Название: " + attraction.getName());
        System.out.println("  Owner: " + (attraction.getOwner() != null ? attraction.getOwner().getUsername() + " (ID=" + attraction.getOwner().getId() + ")" : "null"));
        try {
            Attraction saved = attractionRepository.save(attraction);
            System.out.println("✅ AttractionService.saveAttraction: сохранено с ID=" + saved.getId());
            System.out.println("  Сохранённый Owner: " + (saved.getOwner() != null ? saved.getOwner().getUsername() + " (ID=" + saved.getOwner().getId() + ")" : "null"));
            return saved;
        } catch (Exception e) {
            System.err.println("❌ ОШИБКА в AttractionService.saveAttraction: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    public void deleteAttraction(Long id) {
        System.out.println("AttractionService.deleteAttraction: удаление аттракциона ID=" + id);
        attractionRepository.deleteById(id);
        System.out.println("AttractionService.deleteAttraction: удаление выполнено");
    }
    
    public List<Attraction> findByType(String type) {
        return attractionRepository.findByType(type);
    }
    
    public List<Attraction> findByStatus(String status) {
        return attractionRepository.findByStatus(status);
    }
    
    public List<Attraction> searchByName(String name) {
        return attractionRepository.findByNameContainingIgnoreCase(name);
    }
    
    public List<Attraction> searchAttractions(String name, String type, String status, String location) {
        return attractionRepository.searchAttractions(
            name != null && !name.trim().isEmpty() ? name : null,
            type != null && !type.trim().isEmpty() ? type : null,
            status != null && !status.trim().isEmpty() ? status : null,
            location != null && !location.trim().isEmpty() ? location : null
        );
    }
    
    public long getTotalCount() {
        long count = attractionRepository.count();
        System.out.println("AttractionService.getTotalCount: всего " + count + " аттракционов в БД");
        return count;
    }
    
    public long getCountByStatus(String status) {
        return attractionRepository.findByStatus(status).size();
    }
    
    public long getCountByType(String type) {
        return attractionRepository.findByType(type).size();
    }
}

