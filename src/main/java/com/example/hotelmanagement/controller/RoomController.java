package com.example.hotelmanagement.controller;

import com.example.hotelmanagement.exception.PhotoRetrievalException;
import com.example.hotelmanagement.exception.ResourceNotFoundException;
import com.example.hotelmanagement.model.BookedRoom;
import com.example.hotelmanagement.model.Room;
import com.example.hotelmanagement.request.UpdateRoomRequest;
import com.example.hotelmanagement.response.BookingResponse;
import com.example.hotelmanagement.response.RoomResponse;
import com.example.hotelmanagement.service.BookingService;
import com.example.hotelmanagement.service.IRoomService;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rooms")
public class RoomController {
    private final IRoomService roomService;
    private final BookingService bookingService;

    @PostMapping("/add/new-room")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<RoomResponse> addNewRoom(
                        @ModelAttribute UpdateRoomRequest updateRoomRequest) throws SQLException, IOException {
        MultipartFile photo = updateRoomRequest.getPhoto();
        String roomType = updateRoomRequest.getRoomType();
        BigDecimal roomPrice = updateRoomRequest.getRoomPrice();
        Room savedRoom=roomService.addNewRoom(photo,roomType,roomPrice);
        RoomResponse response=new RoomResponse(savedRoom.getId(),savedRoom.getRoomType(),savedRoom.getRoomPrice());
        return ResponseEntity.ok(response);
    }
    @GetMapping("/room/types")
    public List<String> getRoomTypes(){
        return roomService.getAllRoomTypes();
    }
    @GetMapping("/all-rooms")
    public  ResponseEntity<List<RoomResponse>> getAllRooms() throws SQLException {
        List<Room> rooms=roomService.getAllRooms();
        List<RoomResponse> roomResponses=new ArrayList<>();
        for(Room room:rooms){
            byte[] photoBytes=roomService.getRoomPhotoByRoomId(room.getId());
            if(photoBytes!=null && photoBytes.length>0){
                String base64Photo= Base64.encodeBase64String(photoBytes);
                RoomResponse roomResponse=getRoomResponse(room);
                roomResponse.setPhoto(base64Photo);
                roomResponses.add(roomResponse);
            }
        }
        return ResponseEntity.ok(roomResponses);
    }
    @DeleteMapping("/delete/room/{roomId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long roomId){
        roomService.deleteRoom(roomId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/update/{roomId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public  ResponseEntity<RoomResponse> updateRoom(@PathVariable Long roomId,
                                                    @ModelAttribute UpdateRoomRequest updateRoomRequest) throws IOException, SQLException {
        MultipartFile photo = updateRoomRequest.getPhoto();
        String roomType = updateRoomRequest.getRoomType();
        BigDecimal roomPrice = updateRoomRequest.getRoomPrice();
        System.out.println(photo+" "+roomType+" "+roomPrice);
        byte[] photoBytes=photo!=null && !photo.isEmpty()
                ? photo.getBytes() : roomService.getRoomPhotoByRoomId(roomId);
        Blob photoBlob=photoBytes!=null && photoBytes.length>0
                ? new SerialBlob(photoBytes):null;
        Room theRoom=roomService.updateRoom(roomId,roomType,roomPrice,photoBytes);
        theRoom.setPhoto(photoBlob);
        RoomResponse roomResponse=getRoomResponse(theRoom);
        return ResponseEntity.ok(roomResponse);
    }
    @GetMapping("/room/{roomId}")
    public ResponseEntity<Optional<RoomResponse>> getRoomById(@PathVariable Long roomId){
        Optional<Room> theRoom=roomService.getRoomById(roomId);
        return theRoom.map(room -> {
            RoomResponse roomResponse=getRoomResponse(room);
            return ResponseEntity.ok(Optional.of(roomResponse));
        }).orElseThrow(() -> new ResourceNotFoundException("Room not found"));
    }
    @GetMapping("/available-rooms")
    public ResponseEntity<List<RoomResponse>> getAvailableRooms(
            @RequestParam("checkInDate") @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate checkInDate,
            @RequestParam("checkOutDate") @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate checkOutDate,
            @RequestParam("roomType") String roomType) throws SQLException {
        List<Room> availableRooms=roomService.getAvailableRooms(checkInDate,checkOutDate,roomType);
        List<RoomResponse> roomResponses=new ArrayList<>();
        for(Room room:availableRooms){
            byte[] photoBytes=roomService.getRoomPhotoByRoomId(room.getId());
            if(photoBytes != null && photoBytes.length>0){
                String photoBase64=Base64.encodeBase64String(photoBytes);
                RoomResponse roomResponse=getRoomResponse(room);
                roomResponse.setPhoto(photoBase64);
                roomResponses.add(roomResponse);
            }
        }
        if(roomResponses.isEmpty()){
            return ResponseEntity.noContent().build();
        }else{
            return ResponseEntity.ok(roomResponses);
        }
    }

    private RoomResponse getRoomResponse(Room room) {
        List<BookedRoom> bookings=getAllBookingsByRoomId(room.getId());
        List<BookingResponse> bookingInfo=bookings
                .stream()
                .map(booking->new BookingResponse(booking.getBookingId(),
                        booking.getCheckInDate(),booking.getCheckOutDate(),
                        booking.getBookingConfirmationCode()))
                .toList();
        byte[] photoBytes=null;
        Blob photoBlob=room.getPhoto();
        if(photoBlob!=null){
            try{
                photoBytes=photoBlob.getBytes(1,(int)photoBlob.length());
            }catch (SQLException e){
                throw new PhotoRetrievalException("Error retrieving photo");
            }
        }
        return new RoomResponse(room.getId(),room.getRoomType(),
                room.getRoomPrice(),room.getIsBooked(),photoBytes,bookingInfo);
    }

    private List<BookedRoom> getAllBookingsByRoomId(Long roomId) {
        return bookingService.getAllBookingsByRoomId(roomId);
    }
}
