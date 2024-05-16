package jp.co.metateam.library.controller;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import jp.co.metateam.library.service.AccountService;
import jp.co.metateam.library.service.RentalManageService;
import jp.co.metateam.library.service.StockService;
import lombok.extern.log4j.Log4j2;
import jp.co.metateam.library.model.RentalManage;
import jp.co.metateam.library.values.RentalStatus;
import jp.co.metateam.library.model.Stock;
import jp.co.metateam.library.model.RentalManageDto;
import jp.co.metateam.library.model.Account;




/**
 * 貸出管理関連クラスß
 */
@Log4j2
@Controller
public class RentalManageController {

    private final AccountService accountService;
    private final RentalManageService rentalManageService;
    private final StockService stockService;

    @Autowired
    public RentalManageController(
        AccountService accountService, 
        RentalManageService rentalManageService, 
        StockService stockService
    ) {
        this.accountService = accountService;
        this.rentalManageService = rentalManageService;
        this.stockService = stockService;
    }

    /**
     * 貸出一覧画面初期表示
     * @param model
     * @return
     */
    @GetMapping("/rental/index")
    public String index(Model model) {
        // 貸出管理テーブルから全件取得
        List <RentalManage> rentalManageList = this.rentalManageService.findAll();
    
        // 貸出一覧画面に渡すデータをmodelに追加
        model.addAttribute("rentalManageList", rentalManageList);

        // 貸出一覧画面に遷移
        return "rental/index";
    }

    @GetMapping("/rental/add")
    public String add(Model model) {
        List <Stock> stockList = this.stockService.findAll();
        List <Account> accounts = this.accountService.findAll();

        model.addAttribute("stockList", stockList);
        model.addAttribute("accounts", accounts);
        model.addAttribute("rentalStatus", RentalStatus.values());

        if (!model.containsAttribute("rentalManageDto")) {
            model.addAttribute("rentalManageDto", new RentalManageDto());
        }

        return "rental/add";
    }

    @PostMapping("/rental/add")
    public String save(@Valid @ModelAttribute RentalManageDto rentalManageDto, BindingResult result, RedirectAttributes ra) {
        try {
            if (result.hasErrors()) {
                throw new Exception("Validation error.");
            }
            // 登録処理
            this.rentalManageService.save(rentalManageDto);

            return "redirect:/rental/index";
        } catch (Exception e) {
            log.error(e.getMessage());

            ra.addFlashAttribute("rentalManageDto", rentalManageDto);
            ra.addFlashAttribute("org.springframework.validation.BindingResult.rentalManageDto", result);

            return "redirect:/rental/add";
        }
    }


/*
 *貸出編集画面初期表示
 */

@GetMapping("/rental/{id}/edit")
public String edit(@PathVariable("id") Long id, Model model) {
    List <Stock> stockList = this.stockService.findAll();  //在庫管理番号の情報取得、リスト作成
    List <Account> accounts = this.accountService.findAll(); //社員番号の情報取得、リスト作成

        model.addAttribute("stockList", stockList); //在庫管理番号のリストを表示（プルダウン）
        model.addAttribute("accounts", accounts);  //社員番号のリストを表示（プルダウン）
        model.addAttribute("rentalStatus", RentalStatus.values());  //貸出ステータスリスト（プルダウン）

        RentalManage rentalManage = this.rentalManageService.findById(Long.valueOf(id)); //貸出管理テーブルから{id}の情報を持ってくる

        /*
         * 取得した貸出管理情報をそれぞれセットする
         */
        if (!model.containsAttribute("rentalManageDto")) {
            RentalManageDto rentalManageDto = new RentalManageDto();

        rentalManageDto.setId(rentalManage.getId());
        rentalManageDto.setStatus(rentalManage.getStatus());
        rentalManageDto.setExpectedRentalOn(rentalManage.getExpectedRentalOn());
        rentalManageDto.setExpectedReturnOn(rentalManage.getExpectedReturnOn());
        rentalManageDto.setStockId(rentalManage.getStock().getId());
        rentalManageDto.setEmployeeId(rentalManage.getAccount().getEmployeeId());

        /*
         * セットした内容の表示
         */
        model.addAttribute("rentalManageDto", rentalManageDto);
    }

    return "rental/edit";
 }



@PostMapping("/rental/{id}/edit")
public String update(@PathVariable("id") String id, @Valid @ModelAttribute RentalManageDto rentalManageDto, BindingResult result, Model model) {
    //URLに含まれる動的な引数を受け取るためのアノテーション
    //Valid,ModelAttributeバリデーションを行うことを示す。（バリデーションを行いたいパラメータ・クラスに付与するアノテーション）このアノテーションによって、RentalManageDto内のフィールドに適用されたバリデーションが実行される。
    //BindingResult result: バリデーションの結果が格納される。バリデーションエラーが発生した場合、エラー情報がここに保存される
    //RedirectAttributes ra: リダイレクト先のビューにデータを渡すための属性。リダイレクト先でこの属性に設定されたデータを取得できる
    //Model model: ビューに渡すデータを保持 する。コントローラーで処理された結果をビューに表示するために使用される。

    //System.out.println("ああああああああああああ");
    //System.out.println(rentalManageDto);

    // 変更前のステータス
    // RentalManage rentalManage = rentalManageService.findById(Long.valueOf(id));
    // int beforeStatus = rentalManage.getStatus();
    // 変更後のステータス
    Integer afterStatus = rentalManageDto.getStatus();

     //変更後の貸出予定日の取得
     Date afterexpectedRentalOn = rentalManageDto.getExpectedRentalOn();
     // DateをLocalDateに変換
     LocalDate newexpectedRentalOn = afterexpectedRentalOn.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
     //今日の日付の取得
     LocalDate currentDate = LocalDate.now();

    //  System.out.println("ああああああああああああ");
    //  System.out.println(newexpectedRentalOn);
    //  System.out.println(currentDate);

    try {

        RentalManage rentalManage = this.rentalManageService.findById(Long.valueOf(id));
              //String validerror = rentalManageDto.isStatusError(rentalManage.getStatus());
              String validerror = rentalManageService.isStatusError(rentalManage.getStatus(), afterStatus, newexpectedRentalOn, currentDate);
           
             if (validerror != null) {
                result.addError(new FieldError("rentalManageDto","status",validerror));  //Exceptionを初期化するためにインスタンス作成している              
            }

        if (result.hasErrors()) {
            throw new Exception("Validation error.");
        }
        // 更新処理
        rentalManageService.update(Long.valueOf(id), rentalManageDto);

        return "redirect:/rental/index";
    } catch (Exception e) {
        log.error(e.getMessage());

        model.addAttribute("rentalManageDto", rentalManageDto);
        model.addAttribute("org.springframework.validation.BindingResult.rentalManageDto", result);

        List <Stock> stockList = this.stockService.findAll();  //在庫管理番号の情報取得、リスト作成
        List <Account> accounts = this.accountService.findAll(); //社員番号の情報取得、リスト作成
    
        model.addAttribute("stockList", stockList); //在庫管理番号のリストを表示（プルダウン）
        model.addAttribute("accounts", accounts);  //社員番号のリストを表示（プルダウン）
        model.addAttribute("rentalStatus", RentalStatus.values());  //貸出ステータスリスト（プルダウン）

        return "rental/edit";
    }
}

}