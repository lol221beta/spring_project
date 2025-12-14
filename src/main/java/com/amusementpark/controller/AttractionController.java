package com.amusementpark.controller;

import com.amusementpark.model.Attraction;
import com.amusementpark.model.User;
import com.amusementpark.service.AttractionService;
import com.amusementpark.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/attractions")
public class AttractionController {
    
    private final AttractionService attractionService;
    private final UserService userService;
    
    @Autowired
    public AttractionController(AttractionService attractionService, UserService userService) {
        this.attractionService = attractionService;
        this.userService = userService;
    }
    
    @GetMapping
    public String getAllAttractions(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String location,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir,
            Model model) {
        
        List<Attraction> attractions;
        
        if (name != null || type != null || status != null || location != null) {
            attractions = attractionService.searchAttractions(name, type, status, location);
        } else {
            attractions = attractionService.getAllAttractions();
            System.out.println("AttractionController.getAllAttractions: получено " + attractions.size() + " аттракционов");
        }
        
        if (sortBy != null && !sortBy.isEmpty()) {
            final String finalSortBy = sortBy;
            final String finalSortDir = sortDir;
            attractions = attractions.stream()
                .sorted((a1, a2) -> {
                    int result = 0;
                    switch (finalSortBy) {
                        case "name":
                            result = a1.getName().compareToIgnoreCase(a2.getName());
                            break;
                        case "type":
                            result = a1.getType().compareToIgnoreCase(a2.getType());
                            break;
                        case "status":
                            String s1 = a1.getStatus() != null ? a1.getStatus() : "";
                            String s2 = a2.getStatus() != null ? a2.getStatus() : "";
                            result = s1.compareToIgnoreCase(s2);
                            break;
                        case "capacity":
                            result = a1.getCapacity().compareTo(a2.getCapacity());
                            break;
                        default:
                            result = a1.getId().compareTo(a2.getId());
                    }
                    return finalSortDir.equalsIgnoreCase("desc") ? -result : result;
                })
                .toList();
        }
        
        model.addAttribute("attractions", attractions);
        model.addAttribute("attraction", new Attraction());
        model.addAttribute("searchName", name != null ? name : "");
        model.addAttribute("searchType", type != null ? type : "");
        model.addAttribute("searchStatus", status != null ? status : "");
        model.addAttribute("searchLocation", location != null ? location : "");
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("totalCount", attractionService.getTotalCount());
        
        return "attractions/list";
    }
    
    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public String showNewForm(Model model) {
        model.addAttribute("attraction", new Attraction());
        return "attractions/form";
    }
    
    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public String saveAttraction(@Valid @ModelAttribute Attraction attraction, 
                                BindingResult result, 
                                Model model,
                                RedirectAttributes redirectAttributes,
                                Authentication authentication) {
        System.out.println("=== СОХРАНЕНИЕ АТТРАКЦИОНА ===");
        System.out.println("ID: " + attraction.getId());
        System.out.println("Название: " + attraction.getName());
        System.out.println("Ошибки валидации: " + result.hasErrors());
        
        if (result.hasErrors()) {
            System.out.println("ОШИБКИ ВАЛИДАЦИИ:");
            StringBuilder errorMessages = new StringBuilder("Ошибки валидации: ");
            result.getAllErrors().forEach(error -> {
                String msg = error.getDefaultMessage();
                System.out.println("  - " + msg);
                errorMessages.append(msg).append("; ");
            });
            // При ошибках валидации возвращаемся к списку с конкретными ошибками
            redirectAttributes.addFlashAttribute("errorMessage", errorMessages.toString());
            return "redirect:/attractions";
        }

        try {
            String username = authentication.getName();
            System.out.println("Текущий пользователь: " + username);
            User currentUser = userService.findByUsername(username);
            if (currentUser == null) {
                System.err.println("❌ Пользователь не найден: " + username);
                redirectAttributes.addFlashAttribute("errorMessage", "Пользователь не найден!");
                return "redirect:/attractions";
            }
            System.out.println("Пользователь найден: ID=" + currentUser.getId() + ", Username=" + currentUser.getUsername());
            boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            System.out.println("Является админом: " + isAdmin);

            if (attraction.getId() == null) {
                // создание нового аттракциона — привязываем к текущему пользователю
                System.out.println("Создание нового аттракциона для пользователя: " + username);
                attraction.setOwner(currentUser);
                System.out.println("Owner установлен: " + (attraction.getOwner() != null ? attraction.getOwner().getUsername() : "null"));
            } else {
                // обновление существующего — проверяем право редактирования
                Attraction existing = attractionService.getAttractionById(attraction.getId())
                    .orElse(null);

                if (existing == null) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Аттракцион не найден!");
                    return "redirect:/attractions";
                }

                boolean isOwner = existing.getOwner() != null
                    && existing.getOwner().getId() != null
                    && existing.getOwner().getId().equals(currentUser.getId());

                if (!isAdmin && !isOwner) {
                    redirectAttributes.addFlashAttribute("errorMessage",
                        "Вы можете редактировать только свои аттракционы.");
                    return "redirect:/attractions";
                }

                // запрещаем изменять владельца через форму
                attraction.setOwner(existing.getOwner());
            }

            System.out.println("Перед сохранением в БД. Owner: " + (attraction.getOwner() != null ? attraction.getOwner().getUsername() : "null"));
            Attraction saved = attractionService.saveAttraction(attraction);
            System.out.println("✅ Аттракцион сохранен: ID=" + saved.getId() + ", Owner=" + (saved.getOwner() != null ? saved.getOwner().getUsername() : "null"));
            redirectAttributes.addFlashAttribute("successMessage", 
                "Аттракцион успешно " + (attraction.getId() == null ? "добавлен" : "обновлен") + "!");
        } catch (Exception e) {
            System.err.println("❌ ОШИБКА при сохранении аттракциона: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Ошибка при сохранении: " + e.getMessage());
        }
        return "redirect:/attractions";
    }
    
    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public String showEditForm(@PathVariable Long id,
                               Model model,
                               RedirectAttributes redirectAttributes,
                               Authentication authentication) {
        return attractionService.getAttractionById(id)
            .map(attraction -> {
                String username = authentication.getName();
                User currentUser = userService.findByUsername(username);
                boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

                boolean isOwner = attraction.getOwner() != null
                    && attraction.getOwner().getId() != null
                    && attraction.getOwner().getId().equals(currentUser.getId());

                if (!isAdmin && !isOwner) {
                    redirectAttributes.addFlashAttribute("errorMessage",
                        "Вы можете редактировать только свои аттракционы.");
                    return "redirect:/attractions";
                }

                model.addAttribute("attraction", attraction);
                return "attractions/form";
            })
            .orElseGet(() -> {
                redirectAttributes.addFlashAttribute("errorMessage", "Аттракцион не найден!");
                return "redirect:/attractions";
            });
    }
    
    @PostMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public String deleteAttraction(@PathVariable Long id,
                                   RedirectAttributes redirectAttributes,
                                   Authentication authentication) {
        System.out.println("=== УДАЛЕНИЕ АТТРАКЦИОНА ===");
        System.out.println("ID для удаления: " + id);
        
        return attractionService.getAttractionById(id)
            .map(attraction -> {
                String username = authentication.getName();
                System.out.println("Пользователь: " + username);
                User currentUser = userService.findByUsername(username);
                if (currentUser == null) {
                    System.err.println("Пользователь не найден: " + username);
                    redirectAttributes.addFlashAttribute("errorMessage", "Пользователь не найден!");
                    return "redirect:/attractions";
                }
                boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                System.out.println("Является админом: " + isAdmin);

                boolean isOwner = attraction.getOwner() != null
                    && attraction.getOwner().getId() != null
                    && attraction.getOwner().getId().equals(currentUser.getId());
                System.out.println("Является владельцем: " + isOwner);
                System.out.println("Owner аттракциона: " + (attraction.getOwner() != null ? attraction.getOwner().getUsername() : "null"));

                if (!isAdmin && !isOwner) {
                    System.err.println("Нет прав на удаление!");
                    redirectAttributes.addFlashAttribute("errorMessage",
                        "Вы можете удалять только свои аттракционы.");
                    return "redirect:/attractions";
                }

                try {
                    attractionService.deleteAttraction(id);
                    System.out.println("Аттракцион успешно удален!");
                    redirectAttributes.addFlashAttribute("successMessage", "Аттракцион успешно удален!");
                } catch (Exception e) {
                    System.err.println("ОШИБКА при удалении: " + e.getMessage());
                    e.printStackTrace();
                    redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при удалении: " + e.getMessage());
                }
                return "redirect:/attractions";
            })
            .orElseGet(() -> {
                System.err.println("Аттракцион с ID " + id + " не найден!");
                redirectAttributes.addFlashAttribute("errorMessage", "Аттракцион не найден!");
                return "redirect:/attractions";
            });
    }
    
}

