package com.dsi.studyhub.services;

import com.dsi.studyhub.entities.Client;
import com.dsi.studyhub.repositories.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;


    public Client getMe() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return clientRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Client not found"));
    }


    public void banUser(String clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + clientId));
        client.setBanned(true);
        clientRepository.save(client);
    }

    public void unbanUser(String clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + clientId));
        client.setBanned(false);
        clientRepository.save(client);
    }

    public Client getClientById(String clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + clientId));
    }

    public Page<Client> getAllClients(String firstName, String lastName,
                                      String email, Boolean banned,
                                      int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("firstName").ascending());
        return clientRepository.findWithFilters(firstName, lastName, email, banned, pageable);
    }

}