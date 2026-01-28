package com.aquariux.technical.assessment.trade.mapper;

import com.aquariux.technical.assessment.trade.dto.internal.UserWalletDto;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserWalletMapper {
    
    @Select("""
            SELECT uw.id, uw.user_id as user_id, uw.symbol_id as symbolId, s.symbol, s.name, uw.balance 
            FROM symbols s 
            INNER JOIN user_wallets uw ON s.id = uw.symbol_id AND uw.user_id = #{userId} 
            ORDER BY s.symbol
            """)
    List<UserWalletDto> findByUserId(Long userId);

    @Insert("""
            INSERT INTO user_wallets (user_id, symbol_id, balance)
            VALUES (#{userId}, #{symbolId}, #{balance})
            """)
    void insertUserWallet(UserWalletDto userWalletDto);

    @Update("""
            UPDATE user_wallets
            SET balance = #{balance}, updated_at = #{updatedAt}
            WHERE id = ${id}
            """)
    void updateUserWallet(UserWalletDto userWalletDto);
}