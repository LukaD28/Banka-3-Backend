package rs.raf.user_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import rs.raf.user_service.domain.dto.ClientDto;
import rs.raf.user_service.domain.dto.CreateClientDto;
import rs.raf.user_service.domain.dto.EmailRequestDto;
import rs.raf.user_service.domain.dto.UpdateClientDto;
import rs.raf.user_service.domain.entity.Client;
import rs.raf.user_service.domain.entity.Role;
import rs.raf.user_service.domain.mapper.ClientMapper;
import rs.raf.user_service.repository.AuthTokenRepository;
import rs.raf.user_service.repository.ClientRepository;
import rs.raf.user_service.repository.RoleRepository;
import rs.raf.user_service.repository.UserRepository;
import rs.raf.user_service.service.ClientService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientMapper clientMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AuthTokenRepository authTokenRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ClientService clientService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testListClients() {
        Client client = new Client();
        client.setId(1L);
        Page<Client> page = new PageImpl<>(List.of(client));
        when(clientRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(clientMapper.toDto(any(Client.class))).thenReturn(new ClientDto());

        List<ClientDto> clients = clientService.listClients(PageRequest.of(0, 5)).getContent();
        assertNotNull(clients);
        assertEquals(1, clients.size());
    }

    @Test
    public void testGetClientById_Success() {
        Client client = new Client();
        client.setId(1L);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(clientMapper.toDto(client)).thenReturn(new ClientDto());

        ClientDto clientDTO = clientService.getClientById(1L);
        assertNotNull(clientDTO);
    }

    @Test
    public void testGetClientById_NotFound() {
        when(clientRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> clientService.getClientById(1L));
    }

    @Test
    public void testAddClient_Success() throws ParseException {
        CreateClientDto createClientDTO = new CreateClientDto();
        createClientDTO.setFirstName("Mihailo");
        createClientDTO.setLastName("Petrović");
        createClientDTO.setEmail("mihailo@example.com");
        createClientDTO.setAddress("Bulevar Kralja Aleksandra 73");
        createClientDTO.setPhone("0612345678");
        createClientDTO.setGender("M");
        createClientDTO.setBirthDate(new SimpleDateFormat("yyyy-MM-dd").parse("1995-08-20"));

        Client client = new Client();
        client.setId(1L);
        client.setFirstName(createClientDTO.getFirstName());
        client.setLastName(createClientDTO.getLastName());
        client.setEmail(createClientDTO.getEmail());
        client.setAddress(createClientDTO.getAddress());
        client.setPhone(createClientDTO.getPhone());
        client.setGender(createClientDTO.getGender());
        client.setBirthDate(createClientDTO.getBirthDate());
        client.setPassword(""); // ✅ Lozinka prazna po zahtevu
        client.setJmbg(createClientDTO.getJmbg());

        ClientDto expectedDTO = new ClientDto(
                client.getId(), client.getFirstName(), client.getLastName(),
                client.getEmail(), client.getAddress(),
                client.getPhone(), client.getGender(), client.getBirthDate(), client.getJmbg(), client.getUsername());

        Role expectedRole = new Role();
        expectedRole.setId(1L);
        expectedRole.setName("CLIENT");

        when(clientMapper.fromCreateDto(createClientDTO)).thenReturn(client);
        when(clientRepository.save(client)).thenReturn(client);
        when(clientMapper.toDto(client)).thenReturn(expectedDTO);
        when(clientRepository.findByJmbg(any())).thenReturn(Optional.empty());
        doNothing().when(rabbitTemplate).convertAndSend(eq("set-password"), any(EmailRequestDto.class));
        when(authTokenRepository.save(any())).thenReturn(null);
        when(roleRepository.findByName(any())).thenReturn(Optional.of(expectedRole));

        ClientDto result = clientService.addClient(createClientDTO);

        assertNotNull(result);
        assertEquals("Mihailo", result.getFirstName());
        assertEquals("Petrović", result.getLastName());
        assertEquals("mihailo@example.com", result.getEmail());

        verify(rabbitTemplate).convertAndSend(eq("set-password"), any(EmailRequestDto.class));
        verify(authTokenRepository).save(any());
    }

    @Test
    public void testUpdateClient_Success() throws ParseException {
        UpdateClientDto updateClientDTO = new UpdateClientDto();
        updateClientDTO.setLastName("Perić");
        updateClientDTO.setAddress("Nova Adresa 100");
        updateClientDTO.setPhone("0601234567");
        updateClientDTO.setGender("M");

        Client existingClient = new Client();
        existingClient.setId(1L);
        existingClient.setEmail("stari@example.com"); // Email se NE MENJA
        existingClient.setJmbg("1234567890123");

        Client updatedClient = new Client();
        updatedClient.setId(1L);
        updatedClient.setLastName(updateClientDTO.getLastName());
        updatedClient.setAddress(updateClientDTO.getAddress());
        updatedClient.setPhone(updateClientDTO.getPhone());
        updatedClient.setGender(updateClientDTO.getGender());
        updatedClient.setEmail(existingClient.getEmail()); // Email ostaje isti
        updatedClient.setJmbg(existingClient.getJmbg());

        ClientDto expectedDTO = new ClientDto(
                updatedClient.getId(), updatedClient.getFirstName(), updatedClient.getLastName(),
                updatedClient.getEmail(), updatedClient.getAddress(),
                updatedClient.getPhone(), updatedClient.getGender(), updatedClient.getBirthDate(),
                updatedClient.getJmbg(), updatedClient.getUsername());

        when(clientRepository.findById(1L)).thenReturn(Optional.of(existingClient));
        doAnswer(invocation -> {
            clientMapper.fromUpdateDto(updateClientDTO, existingClient);
            return null;
        }).when(clientMapper).fromUpdateDto(any(UpdateClientDto.class), any(Client.class));
        when(clientRepository.save(existingClient)).thenReturn(updatedClient);
        when(clientMapper.toDto(updatedClient)).thenReturn(expectedDTO);

        ClientDto result = clientService.updateClient(1L, updateClientDTO);

        assertNotNull(result);
        assertEquals("Perić", result.getLastName());
        assertEquals("stari@example.com", result.getEmail());
        assertEquals("Nova Adresa 100", result.getAddress());
    }

    @Test
    public void testDeleteClient_Success() {
        when(clientRepository.existsById(1L)).thenReturn(true);
        doNothing().when(clientRepository).deleteById(1L);
        assertDoesNotThrow(() -> clientService.deleteClient(1L));
    }

    @Test
    public void testDeleteClient_NotFound() {
        when(clientRepository.existsById(1L)).thenReturn(false);
        assertThrows(NoSuchElementException.class, () -> clientService.deleteClient(1L));
    }

    @Test
    public void testListClients_Empty() {
        Page<Client> emptyPage = new PageImpl<>(Collections.emptyList());
        when(clientRepository.findAll(any(PageRequest.class))).thenReturn(emptyPage);

        List<ClientDto> clients = clientService.listClients(PageRequest.of(0, 5)).getContent();
        assertNotNull(clients);
        assertTrue(clients.isEmpty());
    }
}
