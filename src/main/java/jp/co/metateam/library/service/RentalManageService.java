package jp.co.metateam.library.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
// import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
// import org.springframework.ui.Model;
// import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import jp.co.metateam.library.model.Account;
// import jp.co.metateam.library.model.BookMst;
import jp.co.metateam.library.model.RentalManage;
import jp.co.metateam.library.model.RentalManageDto;
import jp.co.metateam.library.model.Stock;
import jp.co.metateam.library.repository.AccountRepository;
import jp.co.metateam.library.repository.RentalManageRepository;
import jp.co.metateam.library.repository.StockRepository;
import jp.co.metateam.library.values.RentalStatus;
// import io.micrometer.common.util.StringUtils;

@Service
public class RentalManageService {

    private final AccountRepository accountRepository;
    private final RentalManageRepository rentalManageRepository;
    private final StockRepository stockRepository;

     @Autowired
    public RentalManageService(
        AccountRepository accountRepository,
        RentalManageRepository rentalManageRepository,
        StockRepository stockRepository
    ) {
        this.accountRepository = accountRepository;
        this.rentalManageRepository = rentalManageRepository;
        this.stockRepository = stockRepository;
    }

    @Transactional
    public List <RentalManage> findAll() {
        List <RentalManage> rentalManageList = this.rentalManageRepository.findAll();

        return rentalManageList;
    }

    @Transactional
    public RentalManage findById(Long id) {
        return this.rentalManageRepository.findById(id).orElse(null);
    }

    @Transactional 
    public void save(RentalManageDto rentalManageDto) throws Exception {
        try {
            Account account = this.accountRepository.findByEmployeeId(rentalManageDto.getEmployeeId()).orElse(null);
            if (account == null) {
                throw new Exception("Account not found.");
            }

            Stock stock = this.stockRepository.findById(rentalManageDto.getStockId()).orElse(null);
            if (stock == null) {
                throw new Exception("Stock not found.");
            }

            RentalManage rentalManage = new RentalManage();
            rentalManage = setRentalStatusDate(rentalManage, rentalManageDto.getStatus());

            rentalManage.setAccount(account);
            rentalManage.setExpectedRentalOn(rentalManageDto.getExpectedRentalOn());
            rentalManage.setExpectedReturnOn(rentalManageDto.getExpectedReturnOn());
            rentalManage.setStatus(rentalManageDto.getStatus());
            rentalManage.setStock(stock);

            // データベースへの保存
            this.rentalManageRepository.save(rentalManage);
        } catch (Exception e) {
            throw e;
        }
    }

    private RentalManage setRentalStatusDate(RentalManage rentalManage, Integer status) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        
        if (status == RentalStatus.RENTAlING.getValue()) {
            rentalManage.setRentaledAt(timestamp);
        } else if (status == RentalStatus.RETURNED.getValue()) {
            rentalManage.setReturnedAt(timestamp);
        } else if (status == RentalStatus.CANCELED.getValue()) {
            rentalManage.setCanceledAt(timestamp);
        }

        return rentalManage;
    }


    @Transactional 
    /*
    トランザクションが正常にコミットされれば、すべての変更がデータベースに反映される。
    ただし、トランザクション内で例外がスローされると、変更内容は取り消される    
    */
    public void update(Long id, RentalManageDto rentalManageDto) throws Exception { //スロー宣言を行うことで、try-catch文がいらない //データベースの RentalManage TBLのレコードを更新したい
        try {
            //this.accountRepositoryは同じクラス内にあるからthisで持ってくることができる
            //選択された社員番号の情報をアカウントテーブルから取得する
            //nullはアカウントテーブルに情報がなくて、取得できなかったという意味
            Account account = this.accountRepository.findByEmployeeId(rentalManageDto.getEmployeeId()).orElse(null); //orElse(null) は、結果が存在しない場合に、デフォルトの値として null を返す
            if (account == null) {
                throw new Exception("Account not found.");
            }
            //this.stockRepositoryは同じクラス内にあるからthisで持ってくることができる
            Stock stock = this.stockRepository.findById(rentalManageDto.getStockId()).orElse(null);
            if (stock == null) {
                throw new Exception("Stock not found.");
            }

            //this.renrtalRepositoryは同じクラス内にあるからthisで持ってくることができる
            RentalManage updateTargetRental = this.rentalManageRepository.findById(id).orElse(null);
            if (updateTargetRental == null) {
                throw new Exception("RentalManage record not found.");
            }

            //RMDtoに変更後のデータのIdが入ってるけど、privateだからgetでデータを持ってくる
            updateTargetRental.setId(rentalManageDto.getId());
            updateTargetRental.setAccount(account); //109でアカウントの情報を既に持ってきているからgetはいらない
            updateTargetRental.setExpectedRentalOn(rentalManageDto.getExpectedRentalOn());
            updateTargetRental.setExpectedReturnOn(rentalManageDto.getExpectedReturnOn());
            updateTargetRental.setStatus(rentalManageDto.getStatus());
            updateTargetRental.setStock(stock);//114で本の情報を既に持ってきているからgetはいらない

            // データベースへの保存
            this.rentalManageRepository.save(updateTargetRental);
        } catch (Exception e) {
            throw e;
        }
    }

    public String isStatusError(Integer beforeStatus, Integer afterStatus, LocalDate newexpectedRentalOn, LocalDate currentDate) {

        // System.out.println("ああああああああああああ");
        // System.out.println(newexpectedRentalOn != currentDate);
        // System.out.println(beforeStatus == RentalStatus.RENT_WAIT.getValue());
        // System.out.println(afterStatus == RentalStatus.RENTAlING.getValue());
        
        //返却済み・キャンセルのステータスは変更できない
        //getValueなのはprivateであるため
        if(beforeStatus == RentalStatus.RETURNED.getValue() || beforeStatus == RentalStatus.CANCELED.getValue()){
            return "返却済み・キャンセルからは貸出ステータスの変更はできません"; 
        //貸出待ちから返却済みに変更できない
        } else if(beforeStatus == RentalStatus.RENT_WAIT.getValue() && afterStatus == RentalStatus.RETURNED.getValue()){
            return "このステータスは無効です"; 
        //貸出中から貸出待ちに変更できない
        } else if(beforeStatus == RentalStatus.RENTAlING.getValue() && afterStatus == RentalStatus.RENT_WAIT.getValue()) {
            return "このステータスは無効です";
        //貸出中からキャンセルに変更できない
        } else if(beforeStatus == RentalStatus.RENTAlING.getValue() && afterStatus == RentalStatus.CANCELED.getValue()){
            return "このステータスは無効です";
            //貸出待ちから貸出中に変更するのは、貸出予定日当日である必要がある
        } else if(!newexpectedRentalOn.isEqual(currentDate) && beforeStatus == RentalStatus.RENT_WAIT.getValue() && afterStatus == RentalStatus.RENTAlING.getValue()){
            return "貸出予定日以外の日に、貸出中に変更することはできません";
        } 
    //エラーがなければ（上記のパターンに引っかからない場合）nullで返す
    return null;
     }

}

